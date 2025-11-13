package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Equipment entities using Panache.
 */
@ApplicationScoped
public class EquipmentRepository implements PanacheRepository<Equipment> {

    /**
     * Find equipment by product ID
     */
    public Optional<Equipment> findByProductId(UUID productId) {
        try {
            Equipment equipment = find("productId = ?1 and isActive = true", productId).firstResult();
            return Optional.ofNullable(equipment);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Find equipment by product ID with pessimistic lock for updates
     */
    @Transactional
    public Optional<Equipment> findByProductIdForUpdate(UUID productId) {
        try {
            Equipment equipment = find("productId = ?1 and isActive = true", productId)
                    .withLock(LockModeType.PESSIMISTIC_WRITE)
                    .firstResult();
            return Optional.ofNullable(equipment);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Find equipment by ID with pessimistic lock for updates
     */
    @Transactional
    public Optional<Equipment> findByIdForUpdate(Long equipmentId) {
        try {
            Equipment equipment = find("id = ?1 and isActive = true", equipmentId)
                    .withLock(LockModeType.PESSIMISTIC_WRITE)
                    .firstResult();
            return Optional.ofNullable(equipment);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Find equipment by cached SKU
     */
    public Optional<Equipment> findByCachedSku(String sku) {
        try {
            Equipment equipment = find("cachedSku = ?1 and isActive = true", sku).firstResult();
            return Optional.ofNullable(equipment);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Find equipment by category with pagination
     */
    public List<Equipment> findByCategory(String category, Page page) {
        return find("cachedCategory = ?1 and isActive = true", category)
                .page(page)
                .list();
    }

    /**
     * Find equipment by warehouse with available stock
     */
    public List<Equipment> findAvailableByWarehouse(String warehouseId) {
        return find("warehouseId = ?1 and isActive = true and availableQuantity > 0", warehouseId)
                .list();
    }

    /**
     * Find equipment with low stock (below threshold)
     */
    public List<Equipment> findLowStockItems(Integer threshold) {
        return find("isActive = true and availableQuantity <= ?1", threshold)
                .list();
    }

    /**
     * Search equipment by multiple criteria
     */
    public List<Equipment> searchEquipment(String category, String brand, String warehouseId, Page page) {
        StringBuilder query = new StringBuilder("isActive = true");
        
        if (category != null && !category.isEmpty()) {
            query.append(" and cachedCategory = '").append(category).append("'");
        }
        if (brand != null && !brand.isEmpty()) {
            query.append(" and cachedBrand = '").append(brand).append("'");
        }
        if (warehouseId != null && !warehouseId.isEmpty()) {
            query.append(" and warehouseId = '").append(warehouseId).append("'");
        }
        
        return find(query.toString())
                .page(page)
                .list();
    }

    /**
     * Update stock quantity atomically
     */
    @Transactional
    public boolean updateStockQuantity(Long equipmentId, Integer quantityChange, Long expectedVersion) {
        String updateQuery = """
                UPDATE Equipment e 
                SET e.availableQuantity = e.availableQuantity + :quantityChange,
                    e.version = e.version + 1
                WHERE e.id = :equipmentId 
                  AND e.version = :expectedVersion
                  AND e.availableQuantity + :quantityChange >= 0
                """;
        
        int updatedRows = getEntityManager()
                .createQuery(updateQuery)
                .setParameter("quantityChange", quantityChange)
                .setParameter("equipmentId", equipmentId)
                .setParameter("expectedVersion", expectedVersion)
                .executeUpdate();
        
        return updatedRows > 0;
    }

    /**
     * Reserve stock atomically
     */
    @Transactional
    public boolean reserveStockAtomic(UUID productId, Integer quantity, Long expectedVersion) {
        String updateQuery = """
                UPDATE Equipment e 
                SET e.availableQuantity = e.availableQuantity - :quantity,
                    e.reservedQuantity = e.reservedQuantity + :quantity,
                    e.version = e.version + 1
                WHERE e.productId = :productId 
                  AND e.version = :expectedVersion
                  AND e.isActive = true
                  AND e.availableQuantity >= :quantity
                """;
        
        int updatedRows = getEntityManager()
                .createQuery(updateQuery)
                .setParameter("quantity", quantity)
                .setParameter("productId", productId)
                .setParameter("expectedVersion", expectedVersion)
                .executeUpdate();
        
        return updatedRows > 0;
    }

    /**
     * Release reserved stock atomically
     */
    @Transactional
    public boolean releaseReservedStockAtomic(UUID productId, Integer quantity, Long expectedVersion) {
        String updateQuery = """
                UPDATE Equipment e 
                SET e.availableQuantity = e.availableQuantity + :quantity,
                    e.reservedQuantity = e.reservedQuantity - :quantity,
                    e.version = e.version + 1
                WHERE e.productId = :productId 
                  AND e.version = :expectedVersion
                  AND e.isActive = true
                  AND e.reservedQuantity >= :quantity
                """;
        
        int updatedRows = getEntityManager()
                .createQuery(updateQuery)
                .setParameter("quantity", quantity)
                .setParameter("productId", productId)
                .setParameter("expectedVersion", expectedVersion)
                .executeUpdate();
        
        return updatedRows > 0;
    }

    /**
     * Advanced equipment search with comprehensive filtering and sorting
     */
    public List<Equipment> searchEquipmentAdvanced(
            String category, String brand, String warehouseId, String equipmentType,
            String availabilityStatus, Integer minQuantity, Boolean isActive, 
            Boolean isRentalAvailable, String minPrice, String maxPrice,
            String sortBy, String sortDirection, Page page) {
        
        StringBuilder query = new StringBuilder("SELECT e FROM Equipment e WHERE 1=1");
        
        // Basic filters
        if (isActive != null) {
            query.append(" AND e.isActive = ").append(isActive);
        } else {
            query.append(" AND e.isActive = true"); // Default to active only
        }
        
        if (category != null && !category.isEmpty()) {
            query.append(" AND e.cachedCategory = '").append(category).append("'");
        }
        
        if (brand != null && !brand.isEmpty()) {
            query.append(" AND e.cachedBrand = '").append(brand).append("'");
        }
        
        if (warehouseId != null && !warehouseId.isEmpty()) {
            query.append(" AND e.warehouseId = '").append(warehouseId).append("'");
        }
        
        if (equipmentType != null && !equipmentType.isEmpty()) {
            query.append(" AND e.cachedEquipmentType = '").append(equipmentType).append("'");
        }
        
        if (isRentalAvailable != null) {
            query.append(" AND e.isRentalAvailable = ").append(isRentalAvailable);
        }
        
        // Availability status filters
        if (availabilityStatus != null && !availabilityStatus.isEmpty()) {
            switch (availabilityStatus.toUpperCase()) {
                case "AVAILABLE" -> query.append(" AND e.availableQuantity > 0");
                case "OUT_OF_STOCK" -> query.append(" AND e.availableQuantity = 0");
                case "LOW_STOCK" -> query.append(" AND e.availableQuantity > 0 AND e.availableQuantity <= 5");
                case "RESERVED" -> query.append(" AND e.reservedQuantity > 0");
                case "HAS_PENDING" -> query.append(" AND e.pendingReservations > 0");
            }
        }
        
        // Quantity filters
        if (minQuantity != null) {
            query.append(" AND e.availableQuantity >= ").append(minQuantity);
        }
        
        // Price range filters
        if (minPrice != null && !minPrice.isEmpty()) {
            try {
                query.append(" AND e.dailyRate >= ").append(Double.parseDouble(minPrice));
            } catch (NumberFormatException e) {
                // Ignore invalid price
            }
        }
        
        if (maxPrice != null && !maxPrice.isEmpty()) {
            try {
                query.append(" AND e.dailyRate <= ").append(Double.parseDouble(maxPrice));
            } catch (NumberFormatException e) {
                // Ignore invalid price
            }
        }
        
        // Sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            String sortField = switch (sortBy.toLowerCase()) {
                case "name" -> "e.cachedName";
                case "price" -> "e.dailyRate";
                case "quantity" -> "e.availableQuantity";
                case "category" -> "e.cachedCategory";
                case "brand" -> "e.cachedBrand";
                case "updated" -> "e.updatedAt";
                default -> "e.cachedName";
            };
            
            String direction = "DESC".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
            query.append(" ORDER BY ").append(sortField).append(" ").append(direction);
        } else {
            query.append(" ORDER BY e.cachedName ASC");
        }
        
        return getEntityManager()
                .createQuery(query.toString(), Equipment.class)
                .setFirstResult(page.index * page.size)
                .setMaxResults(page.size)
                .getResultList();
    }

    /**
     * Find available equipment by warehouse with pagination
     */
    public List<Equipment> findAvailableEquipment(String warehouseId, Page page) {
        StringBuilder query = new StringBuilder("isActive = true AND availableQuantity > 0");
        if (warehouseId != null && !warehouseId.isEmpty()) {
            query.append(" AND warehouseId = '").append(warehouseId).append("'");
        }
        return find(query.toString()).page(page).list();
    }

    /**
     * Find out of stock equipment with pagination
     */
    public List<Equipment> findOutOfStockEquipment(String warehouseId, Page page) {
        StringBuilder query = new StringBuilder("isActive = true AND availableQuantity = 0");
        if (warehouseId != null && !warehouseId.isEmpty()) {
            query.append(" AND warehouseId = '").append(warehouseId).append("'");
        }
        return find(query.toString()).page(page).list();
    }

    /**
     * Find low stock equipment with pagination
     */
    public List<Equipment> findLowStockEquipment(String warehouseId, Integer threshold, Page page) {
        StringBuilder query = new StringBuilder("isActive = true AND availableQuantity > 0 AND availableQuantity <= " + threshold);
        if (warehouseId != null && !warehouseId.isEmpty()) {
            query.append(" AND warehouseId = '").append(warehouseId).append("'");
        }
        return find(query.toString()).page(page).list();
    }

    /**
     * Find equipment with active reservations
     */
    public List<Equipment> findEquipmentWithReservations(String warehouseId, Page page) {
        StringBuilder query = new StringBuilder("isActive = true AND reservedQuantity > 0");
        if (warehouseId != null && !warehouseId.isEmpty()) {
            query.append(" AND warehouseId = '").append(warehouseId).append("'");
        }
        return find(query.toString()).page(page).list();
    }

    /**
     * Find inactive equipment
     */
    public List<Equipment> findInactiveEquipment(String warehouseId, Page page) {
        StringBuilder query = new StringBuilder("isActive = false");
        if (warehouseId != null && !warehouseId.isEmpty()) {
            query.append(" AND warehouseId = '").append(warehouseId).append("'");
        }
        return find(query.toString()).page(page).list();
    }

    /**
     * Find active equipment
     */
    public List<Equipment> findActiveEquipment(String warehouseId, Page page) {
        StringBuilder query = new StringBuilder("isActive = true");
        if (warehouseId != null && !warehouseId.isEmpty()) {
            query.append(" AND warehouseId = '").append(warehouseId).append("'");
        }
        return find(query.toString()).page(page).list();
    }
}
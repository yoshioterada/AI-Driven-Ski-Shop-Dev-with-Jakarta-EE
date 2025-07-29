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
}
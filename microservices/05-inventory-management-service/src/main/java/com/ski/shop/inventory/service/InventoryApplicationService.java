package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.dto.AvailabilityResponse;
import com.ski.shop.inventory.dto.EquipmentDto;
import com.ski.shop.inventory.dto.UpdateStockRequest;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CacheKey;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service for inventory management operations
 */
@ApplicationScoped
public class InventoryApplicationService {

    @Inject
    EquipmentRepository equipmentRepository;

    @Inject
    RedisDataSource redisDataSource;

    private ValueCommands<String, String> redisCommands;
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String CACHE_PREFIX = "inventory:";
    private static final int CACHE_TTL_SECONDS = 1800; // 30 minutes

    public void init() {
        this.redisCommands = redisDataSource.value(String.class);
    }

    /**
     * Get equipment list with optional filtering
     */
    @CacheResult(cacheName = "equipment-list")
    public List<EquipmentDto> getEquipmentList(String category, String brand, String warehouseId, Page page) {
        Log.debugf("Getting equipment list - category: %s, brand: %s, warehouse: %s", category, brand, warehouseId);
        
        List<Equipment> equipment = equipmentRepository.searchEquipment(category, brand, warehouseId, page);
        return equipment.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get detailed equipment information
     */
    @CacheResult(cacheName = "equipment-detail")
    public EquipmentDto getEquipmentDetail(UUID productId) {
        Log.debugf("Getting equipment detail for product ID: %s", productId);
        
        Optional<Equipment> equipment = equipmentRepository.findByProductId(productId);
        return equipment.map(this::convertToDto).orElse(null);
    }

    /**
     * Check equipment availability for a specific period
     */
    public AvailabilityResponse checkAvailability(UUID productId, LocalDateTime startDate, 
                                                LocalDateTime endDate, Integer requiredQuantity) {
        Log.debugf("Checking availability for product ID: %s, quantity: %d", productId, requiredQuantity);
        
        Optional<Equipment> equipmentOpt = equipmentRepository.findByProductId(productId);
        if (equipmentOpt.isEmpty()) {
            return null;
        }
        
        Equipment equipment = equipmentOpt.get();
        boolean isAvailable = equipment.hasAvailableStock(requiredQuantity);
        
        // For Phase 1, we'll provide basic availability without detailed date-by-date analysis
        AvailabilityResponse response = new AvailabilityResponse(
                productId,
                equipment.getTotalQuantity(),
                equipment.availableQuantity,
                equipment.reservedQuantity,
                0, // maintenance quantity - will be implemented in later phases
                isAvailable
        );
        
        // Set warehouse info
        response.warehouseInfo = new AvailabilityResponse.WarehouseInfo(
                equipment.warehouseId,
                "Warehouse " + equipment.warehouseId // Simple naming for now
        );
        
        if (isAvailable) {
            response.nextAvailableDate = LocalDateTime.now();
        } else {
            response.nextAvailableDate = LocalDateTime.now().plusDays(1); // Simplified for Phase 1
        }
        
        return response;
    }

    /**
     * Update stock level for equipment
     */
    @Transactional
    public boolean updateStockLevel(UUID productId, UpdateStockRequest request) {
        Log.infof("Updating stock for product ID: %s, quantity: %d, reason: %s", 
                 productId, request.quantity, request.reason);
        
        Optional<Equipment> equipmentOpt = equipmentRepository.findByProductIdForUpdate(productId);
        if (equipmentOpt.isEmpty()) {
            Log.warnf("Equipment not found for product ID: %s", productId);
            return false;
        }
        
        Equipment equipment = equipmentOpt.get();
        
        // Validate the request
        if (!validateStockOperation(equipment, request)) {
            throw new IllegalArgumentException("Invalid stock operation");
        }
        
        // Apply stock change
        if ("REMOVE".equals(request.reason) || "SOLD".equals(request.reason) || request.quantity < 0) {
            equipment.removeStock(Math.abs(request.quantity));
        } else {
            equipment.addStock(request.quantity);
        }
        
        // Persist the changes
        equipmentRepository.persist(equipment);
        
        Log.infof("Stock updated successfully for product ID: %s. New available quantity: %d", 
                 productId, equipment.availableQuantity);
        
        return true;
    }

    /**
     * Search equipment by multiple criteria
     */
    public List<EquipmentDto> searchEquipment(String sku, String name, String category, String type, Page page) {
        Log.debugf("Searching equipment - sku: %s, name: %s, category: %s, type: %s", sku, name, category, type);
        
        // For Phase 1, we'll implement basic search by category
        // More advanced search features will be added in later phases
        List<Equipment> equipment;
        if (category != null && !category.isEmpty()) {
            equipment = equipmentRepository.findByCategory(category, page);
        } else {
            equipment = equipmentRepository.findAll().page(page).list();
        }
        
        return equipment.stream()
                .filter(eq -> filterBySku(eq, sku))
                .filter(eq -> filterByName(eq, name))
                .filter(eq -> filterByType(eq, type))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Validate stock operation
     */
    private boolean validateStockOperation(Equipment equipment, UpdateStockRequest request) {
        // Basic validation for Phase 1
        if (request.quantity == null || request.quantity == 0) {
            return false;
        }
        
        if (request.reason == null || request.reason.trim().isEmpty()) {
            return false;
        }
        
        // Check for negative stock
        if (("REMOVE".equals(request.reason) || "SOLD".equals(request.reason) || request.quantity < 0) 
            && equipment.availableQuantity < Math.abs(request.quantity)) {
            Log.warnf("Insufficient stock for removal. Available: %d, Requested: %d", 
                     equipment.availableQuantity, Math.abs(request.quantity));
            return false;
        }
        
        return true;
    }

    /**
     * Convert Equipment entity to DTO
     */
    private EquipmentDto convertToDto(Equipment equipment) {
        return new EquipmentDto(
                equipment.productId,
                equipment.cachedSku,
                equipment.cachedName,
                equipment.cachedCategory,
                equipment.cachedBrand,
                equipment.cachedEquipmentType,
                equipment.cachedBasePrice,
                equipment.dailyRate,
                equipment.isRentalAvailable,
                equipment.warehouseId,
                equipment.availableQuantity,
                equipment.reservedQuantity,
                equipment.isActive,
                equipment.updatedAt
        );
    }

    // Helper filter methods
    private boolean filterBySku(Equipment equipment, String sku) {
        return sku == null || sku.isEmpty() || 
               (equipment.cachedSku != null && equipment.cachedSku.toLowerCase().contains(sku.toLowerCase()));
    }

    private boolean filterByName(Equipment equipment, String name) {
        return name == null || name.isEmpty() || 
               (equipment.cachedName != null && equipment.cachedName.toLowerCase().contains(name.toLowerCase()));
    }

    private boolean filterByType(Equipment equipment, String type) {
        return type == null || type.isEmpty() || 
               (equipment.cachedEquipmentType != null && equipment.cachedEquipmentType.equalsIgnoreCase(type));
    }

    /**
     * Advanced equipment list with comprehensive filtering
     */
    @CacheResult(cacheName = "equipment-list-advanced")
    public List<EquipmentDto> getEquipmentListAdvanced(
            String category, String brand, String warehouseId, String equipmentType,
            String availabilityStatus, Integer minQuantity, Boolean isActive, 
            Boolean isRentalAvailable, String minPrice, String maxPrice,
            String sortBy, String sortDirection, Page page) {
        
        Log.debugf("Advanced equipment search with filters");
        
        List<Equipment> equipment = equipmentRepository.searchEquipmentAdvanced(
            category, brand, warehouseId, equipmentType, availabilityStatus,
            minQuantity, isActive, isRentalAvailable, minPrice, maxPrice,
            sortBy, sortDirection, page);
            
        return equipment.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Advanced equipment search with Redis caching
     */
    public List<EquipmentDto> searchEquipmentAdvanced(
            String sku, String name, String category, String type, String brand,
            String availabilityStatus, Integer minAvailable, String minPrice, String maxPrice,
            Boolean hasReservations, Boolean useCache, Page page) {
        
        if (redisCommands == null) {
            init();
        }
        
        String cacheKey = buildSearchCacheKey(sku, name, category, type, brand, 
                                            availabilityStatus, minAvailable, minPrice, 
                                            maxPrice, hasReservations, page);
        
        if (Boolean.TRUE.equals(useCache)) {
            try {
                String cachedResult = redisCommands.get(cacheKey);
                if (cachedResult != null) {
                    Log.debugf("Cache hit for search key: %s", cacheKey);
                    return deserializeEquipmentList(cachedResult);
                }
            } catch (Exception e) {
                Log.warnf("Cache read error: %s", e.getMessage());
            }
        }
        
        // Execute search
        List<Equipment> equipment = equipmentRepository.searchEquipmentAdvanced(
            category, brand, null, type, availabilityStatus,
            minAvailable, null, null, minPrice, maxPrice,
            "name", "ASC", page);
        
        // Apply additional filters
        List<EquipmentDto> results = equipment.stream()
                .filter(eq -> filterBySku(eq, sku))
                .filter(eq -> filterByName(eq, name))
                .filter(eq -> filterByBrand(eq, brand))
                .filter(eq -> filterByReservations(eq, hasReservations))
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        // Cache results if requested
        if (Boolean.TRUE.equals(useCache)) {
            try {
                String serialized = objectMapper.writeValueAsString(results);
                redisCommands.setex(cacheKey, CACHE_TTL_SECONDS, serialized);
                Log.debugf("Cached search results for key: %s", cacheKey);
            } catch (Exception e) {
                Log.warnf("Cache write error: %s", e.getMessage());
            }
        }
        
        return results;
    }

    /**
     * Get equipment filtered by inventory status with caching
     */
    public List<EquipmentDto> getEquipmentByStatus(String status, String warehouseId, 
                                                 Boolean includeReservations, Boolean useCache, Page page) {
        
        if (redisCommands == null) {
            init();
        }
        
        String cacheKey = CACHE_PREFIX + "status:" + status + ":" + warehouseId + ":" + 
                         includeReservations + ":" + page.index + ":" + page.size;
        
        if (Boolean.TRUE.equals(useCache)) {
            try {
                String cachedResult = redisCommands.get(cacheKey);
                if (cachedResult != null) {
                    Log.debugf("Cache hit for status key: %s", cacheKey);
                    return deserializeEquipmentList(cachedResult);
                }
            } catch (Exception e) {
                Log.warnf("Cache read error: %s", e.getMessage());
            }
        }
        
        // Execute query based on status
        List<Equipment> equipment = switch (status.toUpperCase()) {
            case "AVAILABLE" -> equipmentRepository.findAvailableEquipment(warehouseId, page);
            case "OUT_OF_STOCK" -> equipmentRepository.findOutOfStockEquipment(warehouseId, page);
            case "LOW_STOCK" -> equipmentRepository.findLowStockEquipment(warehouseId, 5, page); // Threshold of 5
            case "RESERVED" -> equipmentRepository.findEquipmentWithReservations(warehouseId, page);
            case "INACTIVE" -> equipmentRepository.findInactiveEquipment(warehouseId, page);
            default -> equipmentRepository.findActiveEquipment(warehouseId, page);
        };
        
        List<EquipmentDto> results = equipment.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        // Cache results
        if (Boolean.TRUE.equals(useCache)) {
            try {
                String serialized = objectMapper.writeValueAsString(results);
                redisCommands.setex(cacheKey, CACHE_TTL_SECONDS, serialized);
                Log.debugf("Cached status results for key: %s", cacheKey);
            } catch (Exception e) {
                Log.warnf("Cache write error: %s", e.getMessage());
            }
        }
        
        return results;
    }

    /**
     * Get comprehensive inventory statistics with caching
     */
    public Map<String, Object> getInventoryStatistics(String warehouseId, Boolean useCache) {
        if (redisCommands == null) {
            init();
        }
        
        String cacheKey = CACHE_PREFIX + "stats:" + (warehouseId != null ? warehouseId : "all");
        
        if (Boolean.TRUE.equals(useCache)) {
            try {
                String cachedResult = redisCommands.get(cacheKey);
                if (cachedResult != null) {
                    Log.debugf("Cache hit for statistics key: %s", cacheKey);
                    return objectMapper.readValue(cachedResult, Map.class);
                }
            } catch (Exception e) {
                Log.warnf("Cache read error: %s", e.getMessage());
            }
        }
        
        // Calculate statistics
        Map<String, Object> stats = new HashMap<>();
        
        List<Equipment> allEquipment = warehouseId != null 
            ? Equipment.find("warehouseId = ?1 and isActive = true", warehouseId).list()
            : Equipment.find("isActive = true").list();
        
        stats.put("total_equipment", allEquipment.size());
        stats.put("total_available_quantity", allEquipment.stream()
                .mapToInt(eq -> eq.availableQuantity).sum());
        stats.put("total_reserved_quantity", allEquipment.stream()
                .mapToInt(eq -> eq.reservedQuantity).sum());
        stats.put("total_pending_reservations", allEquipment.stream()
                .mapToInt(eq -> eq.pendingReservations).sum());
        
        long availableItems = allEquipment.stream()
                .filter(eq -> eq.availableQuantity > 0).count();
        long outOfStockItems = allEquipment.stream()
                .filter(eq -> eq.availableQuantity == 0).count();
        long lowStockItems = allEquipment.stream()
                .filter(eq -> eq.availableQuantity > 0 && eq.availableQuantity <= 5).count();
        
        stats.put("available_items", availableItems);
        stats.put("out_of_stock_items", outOfStockItems);
        stats.put("low_stock_items", lowStockItems);
        
        // Category breakdown
        Map<String, Long> categoryBreakdown = allEquipment.stream()
                .filter(eq -> eq.cachedCategory != null)
                .collect(Collectors.groupingBy(
                    eq -> eq.cachedCategory,
                    Collectors.counting()
                ));
        stats.put("category_breakdown", categoryBreakdown);
        
        // Value statistics
        BigDecimal totalInventoryValue = allEquipment.stream()
                .filter(eq -> eq.cachedBasePrice != null)
                .map(eq -> eq.cachedBasePrice.multiply(BigDecimal.valueOf(eq.getTotalQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("total_inventory_value", totalInventoryValue);
        
        stats.put("last_updated", LocalDateTime.now());
        
        // Cache results
        if (Boolean.TRUE.equals(useCache)) {
            try {
                String serialized = objectMapper.writeValueAsString(stats);
                redisCommands.setex(cacheKey, CACHE_TTL_SECONDS / 2, serialized); // Shorter TTL for stats
                Log.debugf("Cached statistics for key: %s", cacheKey);
            } catch (Exception e) {
                Log.warnf("Cache write error: %s", e.getMessage());
            }
        }
        
        return stats;
    }

    // Helper methods for caching
    private String buildSearchCacheKey(String sku, String name, String category, String type, 
                                     String brand, String availabilityStatus, Integer minAvailable,
                                     String minPrice, String maxPrice, Boolean hasReservations, Page page) {
        return CACHE_PREFIX + "search:" + 
               (sku != null ? sku : "") + ":" +
               (name != null ? name : "") + ":" +
               (category != null ? category : "") + ":" +
               (type != null ? type : "") + ":" +
               (brand != null ? brand : "") + ":" +
               (availabilityStatus != null ? availabilityStatus : "") + ":" +
               (minAvailable != null ? minAvailable : "") + ":" +
               (minPrice != null ? minPrice : "") + ":" +
               (maxPrice != null ? maxPrice : "") + ":" +
               (hasReservations != null ? hasReservations : "") + ":" +
               page.index + ":" + page.size;
    }

    private List<EquipmentDto> deserializeEquipmentList(String json) {
        try {
            return objectMapper.readValue(json, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, EquipmentDto.class));
        } catch (Exception e) {
            Log.warnf("Failed to deserialize equipment list: %s", e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean filterByBrand(Equipment equipment, String brand) {
        return brand == null || brand.isEmpty() || 
               (equipment.cachedBrand != null && equipment.cachedBrand.toLowerCase().contains(brand.toLowerCase()));
    }

    private boolean filterByReservations(Equipment equipment, Boolean hasReservations) {
        if (hasReservations == null) return true;
        return hasReservations ? equipment.reservedQuantity > 0 : equipment.reservedQuantity == 0;
    }
}
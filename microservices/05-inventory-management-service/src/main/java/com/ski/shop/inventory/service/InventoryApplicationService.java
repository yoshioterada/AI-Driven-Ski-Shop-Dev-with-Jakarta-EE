package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.dto.AvailabilityResponse;
import com.ski.shop.inventory.dto.EquipmentDto;
import com.ski.shop.inventory.dto.UpdateStockRequest;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for inventory management operations
 */
@ApplicationScoped
public class InventoryApplicationService {

    @Inject
    EquipmentRepository equipmentRepository;

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
}
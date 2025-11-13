package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.dto.EquipmentDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.logging.Log;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing inventory synchronization and data consistency
 */
@ApplicationScoped
public class SynchronizationService {

    @Inject
    AlertService alertService;

    @Inject
    InventoryApplicationService inventoryService;

    /**
     * Perform manual synchronization with Product Catalog Service
     */
    @Transactional
    public SyncResult performManualSync() {
        Log.info("Starting manual inventory synchronization");
        
        SyncResult result = new SyncResult();
        result.syncStartTime = LocalDateTime.now();
        
        try {
            // Simulate Product Catalog Service integration
            // In real implementation, this would call the Product Catalog Service API
            List<ProductCatalogItem> catalogItems = fetchProductCatalogItems();
            
            result.catalogItemsFound = catalogItems.size();
            
            for (ProductCatalogItem catalogItem : catalogItems) {
                try {
                    syncEquipmentFromCatalog(catalogItem, result);
                } catch (Exception e) {
                    Log.error("Failed to sync equipment: " + catalogItem.productId, e);
                    result.syncErrors.add("Failed to sync " + catalogItem.name + ": " + e.getMessage());
                }
            }
            
            // Detect orphaned equipment (exists in inventory but not in catalog)
            detectOrphanedEquipment(catalogItems, result);
            
            result.syncEndTime = LocalDateTime.now();
            result.success = result.syncErrors.isEmpty();
            
            Log.info("Manual synchronization completed. Success: " + result.success + 
                    ", Updated: " + result.equipmentUpdated + 
                    ", Created: " + result.equipmentCreated +
                    ", Errors: " + result.syncErrors.size());
            
        } catch (Exception e) {
            Log.error("Manual synchronization failed", e);
            result.success = false;
            result.syncErrors.add("Synchronization failed: " + e.getMessage());
            result.syncEndTime = LocalDateTime.now();
            
            // Create sync failure alert
            alertService.createSyncFailureAlert(e.getMessage(), "Manual sync failed");
        }
        
        return result;
    }

    /**
     * Detect differences between inventory and catalog
     */
    public List<InventoryDifference> detectDifferences() {
        List<InventoryDifference> differences = new ArrayList<>();
        
        try {
            List<ProductCatalogItem> catalogItems = fetchProductCatalogItems();
            Map<UUID, ProductCatalogItem> catalogMap = catalogItems.stream()
                    .collect(Collectors.toMap(item -> item.productId, item -> item));
            
            List<Equipment> inventoryItems = Equipment.listAll();
            
            // Check for missing items in inventory
            for (ProductCatalogItem catalogItem : catalogItems) {
                Optional<Equipment> equipmentOpt = inventoryItems.stream()
                        .filter(eq -> eq.productId.equals(catalogItem.productId))
                        .findFirst();
                
                if (equipmentOpt.isEmpty()) {
                    differences.add(new InventoryDifference(
                            InventoryDifference.DifferenceType.MISSING_IN_INVENTORY,
                            catalogItem.productId,
                            catalogItem.name,
                            "Product exists in catalog but not in inventory"
                    ));
                } else {
                    // Check for data mismatches
                    Equipment equipment = equipmentOpt.get();
                    List<String> mismatches = new ArrayList<>();
                    
                    if (!Objects.equals(equipment.cachedName, catalogItem.name)) {
                        mismatches.add("Name: '" + equipment.cachedName + "' vs '" + catalogItem.name + "'");
                    }
                    if (!Objects.equals(equipment.cachedCategory, catalogItem.category)) {
                        mismatches.add("Category: '" + equipment.cachedCategory + "' vs '" + catalogItem.category + "'");
                    }
                    if (!Objects.equals(equipment.cachedBrand, catalogItem.brand)) {
                        mismatches.add("Brand: '" + equipment.cachedBrand + "' vs '" + catalogItem.brand + "'");
                    }
                    
                    if (!mismatches.isEmpty()) {
                        differences.add(new InventoryDifference(
                                InventoryDifference.DifferenceType.DATA_MISMATCH,
                                catalogItem.productId,
                                catalogItem.name,
                                "Data mismatches: " + String.join(", ", mismatches)
                        ));
                    }
                }
            }
            
            // Check for orphaned items in inventory
            Set<UUID> catalogProductIds = catalogMap.keySet();
            for (Equipment equipment : inventoryItems) {
                if (!catalogProductIds.contains(equipment.productId)) {
                    differences.add(new InventoryDifference(
                            InventoryDifference.DifferenceType.ORPHANED_IN_INVENTORY,
                            equipment.productId,
                            equipment.cachedName,
                            "Equipment exists in inventory but not in catalog"
                    ));
                }
            }
            
        } catch (Exception e) {
            Log.error("Failed to detect differences", e);
            differences.add(new InventoryDifference(
                    InventoryDifference.DifferenceType.SYNC_ERROR,
                    null,
                    "System Error",
                    "Failed to detect differences: " + e.getMessage()
            ));
        }
        
        return differences;
    }

    /**
     * Perform consistency check on inventory data
     */
    public ConsistencyCheckResult performConsistencyCheck() {
        ConsistencyCheckResult result = new ConsistencyCheckResult();
        result.checkStartTime = LocalDateTime.now();
        
        try {
            List<Equipment> allEquipment = Equipment.listAll();
            
            for (Equipment equipment : allEquipment) {
                // Check data consistency
                List<String> issues = checkEquipmentConsistency(equipment);
                
                if (!issues.isEmpty()) {
                    result.inconsistencies.add(new DataInconsistency(
                            equipment.id,
                            equipment.productId,
                            equipment.cachedName,
                            issues
                    ));
                }
            }
            
            // Check database constraints and relationships
            checkDatabaseConsistency(result);
            
            result.checkEndTime = LocalDateTime.now();
            result.totalEquipmentChecked = allEquipment.size();
            result.inconsistenciesFound = result.inconsistencies.size();
            
        } catch (Exception e) {
            Log.error("Consistency check failed", e);
            result.inconsistencies.add(new DataInconsistency(
                    null, null, "System Error", 
                    Arrays.asList("Consistency check failed: " + e.getMessage())
            ));
        }
        
        return result;
    }

    /**
     * Fix detected inconsistencies
     */
    @Transactional
    public FixResult fixInconsistencies(List<Long> equipmentIds) {
        FixResult result = new FixResult();
        result.fixStartTime = LocalDateTime.now();
        
        for (Long equipmentId : equipmentIds) {
            try {
                Equipment equipment = Equipment.findById(equipmentId);
                if (equipment == null) {
                    result.errors.add("Equipment not found: " + equipmentId);
                    continue;
                }
                
                // Attempt to fix inconsistencies
                boolean fixed = fixEquipmentInconsistencies(equipment);
                
                if (fixed) {
                    result.fixedEquipment.add(equipmentId);
                } else {
                    result.errors.add("Could not fix inconsistencies for equipment: " + equipmentId);
                }
                
            } catch (Exception e) {
                Log.error("Failed to fix equipment inconsistencies: " + equipmentId, e);
                result.errors.add("Failed to fix equipment " + equipmentId + ": " + e.getMessage());
            }
        }
        
        result.fixEndTime = LocalDateTime.now();
        return result;
    }

    // Private helper methods

    private List<ProductCatalogItem> fetchProductCatalogItems() {
        // Simulate fetching from Product Catalog Service
        // In real implementation, this would make HTTP calls to the Product Catalog Service
        List<ProductCatalogItem> items = new ArrayList<>();
        
        // Add some sample data for demonstration
        items.add(new ProductCatalogItem(UUID.randomUUID(), "SKI-001", "Alpine Ski Pro", "Skis", "Alpine", "ProBrand"));
        items.add(new ProductCatalogItem(UUID.randomUUID(), "BOOT-001", "Ski Boot Comfort", "Boots", "Alpine", "ComfortBrand"));
        
        return items;
    }

    private void syncEquipmentFromCatalog(ProductCatalogItem catalogItem, SyncResult result) {
        Optional<Equipment> existingEquipment = Equipment.find("productId", catalogItem.productId)
                .firstResultOptional();
        
        if (existingEquipment.isPresent()) {
            // Update existing equipment
            Equipment equipment = existingEquipment.get();
            boolean updated = false;
            
            if (!Objects.equals(equipment.cachedName, catalogItem.name)) {
                equipment.cachedName = catalogItem.name;
                updated = true;
            }
            if (!Objects.equals(equipment.cachedCategory, catalogItem.category)) {
                equipment.cachedCategory = catalogItem.category;
                updated = true;
            }
            if (!Objects.equals(equipment.cachedBrand, catalogItem.brand)) {
                equipment.cachedBrand = catalogItem.brand;
                updated = true;
            }
            if (!Objects.equals(equipment.cachedSku, catalogItem.sku)) {
                equipment.cachedSku = catalogItem.sku;
                updated = true;
            }
            
            if (updated) {
                equipment.persist();
                result.equipmentUpdated++;
            }
        } else {
            // Create new equipment
            Equipment newEquipment = new Equipment();
            newEquipment.productId = catalogItem.productId;
            newEquipment.cachedSku = catalogItem.sku;
            newEquipment.cachedName = catalogItem.name;
            newEquipment.cachedCategory = catalogItem.category;
            newEquipment.cachedBrand = catalogItem.brand;
            newEquipment.cachedEquipmentType = catalogItem.type;
            newEquipment.dailyRate = new java.math.BigDecimal("50.00"); // Default rate
            newEquipment.availableQuantity = 0;
            newEquipment.reservedQuantity = 0;
            
            newEquipment.persist();
            result.equipmentCreated++;
        }
    }

    private void detectOrphanedEquipment(List<ProductCatalogItem> catalogItems, SyncResult result) {
        Set<UUID> catalogProductIds = catalogItems.stream()
                .map(item -> item.productId)
                .collect(Collectors.toSet());
        
        List<Equipment> orphanedEquipment = Equipment.list("productId NOT IN ?1", catalogProductIds);
        result.orphanedEquipment = orphanedEquipment.size();
        
        if (!orphanedEquipment.isEmpty()) {
            result.syncErrors.add("Found " + orphanedEquipment.size() + " orphaned equipment items");
        }
    }

    private List<String> checkEquipmentConsistency(Equipment equipment) {
        List<String> issues = new ArrayList<>();
        
        // Check for null or empty required fields
        if (equipment.productId == null) {
            issues.add("Missing productId");
        }
        if (equipment.dailyRate == null || equipment.dailyRate.compareTo(java.math.BigDecimal.ZERO) < 0) {
            issues.add("Invalid daily rate");
        }
        if (equipment.availableQuantity == null || equipment.availableQuantity < 0) {
            issues.add("Invalid available quantity");
        }
        if (equipment.reservedQuantity == null || equipment.reservedQuantity < 0) {
            issues.add("Invalid reserved quantity");
        }
        Integer totalQuantity = equipment.getTotalQuantity();
        if (totalQuantity < 0) {
            issues.add("Invalid total quantity");
        }
        
        return issues;
    }

    private void checkDatabaseConsistency(ConsistencyCheckResult result) {
        // Check for equipment with negative quantities
        long negativeQuantities = Equipment.count("availableQuantity < 0 OR reservedQuantity < 0");
        if (negativeQuantities > 0) {
            result.inconsistencies.add(new DataInconsistency(
                    null, null, "Database Consistency",
                    Arrays.asList("Found " + negativeQuantities + " equipment with negative quantities")
            ));
        }
    }

    private boolean fixEquipmentInconsistencies(Equipment equipment) {
        boolean fixed = false;
        
        // Fix negative quantities
        if (equipment.availableQuantity < 0) {
            equipment.availableQuantity = 0;
            fixed = true;
        }
        if (equipment.reservedQuantity < 0) {
            equipment.reservedQuantity = 0;
            fixed = true;
        }
        
        // Fix null daily rate
        if (equipment.dailyRate == null) {
            equipment.dailyRate = new java.math.BigDecimal("50.00");
            fixed = true;
        }
        
        if (fixed) {
            equipment.persist();
        }
        
        return fixed;
    }

    // Helper classes
    public static class ProductCatalogItem {
        public UUID productId;
        public String sku;
        public String name;
        public String category;
        public String type;
        public String brand;
        
        public ProductCatalogItem(UUID productId, String sku, String name, String category, String type, String brand) {
            this.productId = productId;
            this.sku = sku;
            this.name = name;
            this.category = category;
            this.type = type;
            this.brand = brand;
        }
    }

    public static class SyncResult {
        public LocalDateTime syncStartTime;
        public LocalDateTime syncEndTime;
        public boolean success;
        public int catalogItemsFound;
        public int equipmentCreated;
        public int equipmentUpdated;
        public int orphanedEquipment;
        public List<String> syncErrors = new ArrayList<>();
    }

    public static class InventoryDifference {
        public DifferenceType type;
        public UUID productId;
        public String name;
        public String description;
        
        public InventoryDifference(DifferenceType type, UUID productId, String name, String description) {
            this.type = type;
            this.productId = productId;
            this.name = name;
            this.description = description;
        }
        
        public enum DifferenceType {
            MISSING_IN_INVENTORY,
            ORPHANED_IN_INVENTORY,
            DATA_MISMATCH,
            SYNC_ERROR
        }
    }

    public static class ConsistencyCheckResult {
        public LocalDateTime checkStartTime;
        public LocalDateTime checkEndTime;
        public int totalEquipmentChecked;
        public int inconsistenciesFound;
        public List<DataInconsistency> inconsistencies = new ArrayList<>();
    }

    public static class DataInconsistency {
        public Long equipmentId;
        public UUID productId;
        public String equipmentName;
        public List<String> issues;
        
        public DataInconsistency(Long equipmentId, UUID productId, String equipmentName, List<String> issues) {
            this.equipmentId = equipmentId;
            this.productId = productId;
            this.equipmentName = equipmentName;
            this.issues = issues;
        }
    }

    public static class FixResult {
        public LocalDateTime fixStartTime;
        public LocalDateTime fixEndTime;
        public List<Long> fixedEquipment = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
    }
}
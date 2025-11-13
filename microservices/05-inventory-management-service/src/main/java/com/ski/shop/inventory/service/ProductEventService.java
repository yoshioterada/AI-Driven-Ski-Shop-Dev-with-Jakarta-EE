package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.event.ProductEvents;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for handling Product Catalog events
 */
@ApplicationScoped
public class ProductEventService {

    @Inject
    EquipmentRepository equipmentRepository;

    /**
     * Handle ProductCreated event from Product Catalog Service
     */
    @Transactional
    public void handleProductCreated(ProductEvents.ProductCreatedEvent event) {
        Log.infof("Creating new equipment for product ID: %s", event.productId);
        
        // Check if equipment already exists (idempotency)
        Optional<Equipment> existing = equipmentRepository.findByProductId(event.productId);
        if (existing.isPresent()) {
            Log.warnf("Equipment already exists for product ID: %s, skipping creation", event.productId);
            return;
        }
        
        // Calculate default daily rate based on base price
        BigDecimal dailyRate = calculateDefaultDailyRate(event.basePrice, event.category);
        
        // Create new equipment
        Equipment equipment = new Equipment(event.productId, "warehouse-001", dailyRate);
        equipment.updateCachedProductInfo(
                event.sku,
                event.name,
                event.category,
                event.brand,
                event.equipmentType,
                event.basePrice
        );
        
        equipmentRepository.persist(equipment);
        
        Log.infof("Successfully created equipment for product ID: %s", event.productId);
    }

    /**
     * Handle ProductUpdated event from Product Catalog Service
     */
    @Transactional
    public void handleProductUpdated(ProductEvents.ProductUpdatedEvent event) {
        Log.infof("Updating equipment for product ID: %s", event.productId);
        
        Optional<Equipment> equipmentOpt = equipmentRepository.findByProductId(event.productId);
        if (equipmentOpt.isEmpty()) {
            Log.warnf("Equipment not found for product ID: %s, cannot update", event.productId);
            return;
        }
        
        Equipment equipment = equipmentOpt.get();
        equipment.updateCachedProductInfo(
                event.sku,
                event.name,
                event.category,
                event.brand,
                event.equipmentType,
                event.basePrice
        );
        
        equipmentRepository.persist(equipment);
        
        Log.infof("Successfully updated equipment for product ID: %s", event.productId);
    }

    /**
     * Handle ProductDeleted event from Product Catalog Service
     */
    @Transactional
    public void handleProductDeleted(ProductEvents.ProductDeletedEvent event) {
        Log.infof("Deactivating equipment for product ID: %s", event.productId);
        
        Optional<Equipment> equipmentOpt = equipmentRepository.findByProductId(event.productId);
        if (equipmentOpt.isEmpty()) {
            Log.warnf("Equipment not found for product ID: %s, cannot delete", event.productId);
            return;
        }
        
        Equipment equipment = equipmentOpt.get();
        
        // Check if there are active reservations
        if (equipment.reservedQuantity > 0) {
            Log.warnf("Equipment has reserved quantity (%d), cannot fully deactivate product ID: %s", 
                     equipment.reservedQuantity, event.productId);
            // In a real scenario, we would need to handle active reservations properly
            // For now, we'll just mark as not available for new rentals
            equipment.isRentalAvailable = false;
        } else {
            equipment.deactivate();
        }
        
        equipmentRepository.persist(equipment);
        
        Log.infof("Successfully deactivated equipment for product ID: %s", event.productId);
    }

    /**
     * Handle ProductPriceChanged event from Product Catalog Service
     */
    @Transactional
    public void handleProductPriceChanged(ProductEvents.ProductPriceChangedEvent event) {
        Log.infof("Updating price for product ID: %s from %s to %s", 
                 event.productId, event.oldPrice, event.newPrice);
        
        Optional<Equipment> equipmentOpt = equipmentRepository.findByProductId(event.productId);
        if (equipmentOpt.isEmpty()) {
            Log.warnf("Equipment not found for product ID: %s, cannot update price", event.productId);
            return;
        }
        
        Equipment equipment = equipmentOpt.get();
        equipment.cachedBasePrice = event.newPrice;
        equipment.cacheUpdatedAt = LocalDateTime.now();
        
        // Optionally update daily rate based on new base price
        String category = equipment.cachedCategory;
        BigDecimal newDailyRate = calculateDefaultDailyRate(event.newPrice, category);
        equipment.dailyRate = newDailyRate;
        
        equipmentRepository.persist(equipment);
        
        Log.infof("Successfully updated price for product ID: %s", event.productId);
    }

    /**
     * Calculate default daily rental rate based on base price and category
     */
    private BigDecimal calculateDefaultDailyRate(BigDecimal basePrice, String category) {
        if (basePrice == null) {
            return BigDecimal.valueOf(50.00); // Default rate
        }
        
        // Calculate daily rate as percentage of base price
        BigDecimal rate = switch (category != null ? category.toUpperCase() : "OTHER") {
            case "SKI_BOARD", "SNOWBOARD" -> basePrice.multiply(BigDecimal.valueOf(0.08)); // 8% of base price
            case "SKI_BOOTS", "SKI_BINDINGS" -> basePrice.multiply(BigDecimal.valueOf(0.10)); // 10% of base price
            case "SKI_POLES", "ACCESSORIES" -> basePrice.multiply(BigDecimal.valueOf(0.15)); // 15% of base price
            case "HELMETS", "PROTECTIVE_GEAR" -> basePrice.multiply(BigDecimal.valueOf(0.12)); // 12% of base price
            default -> basePrice.multiply(BigDecimal.valueOf(0.10)); // Default 10%
        };
        
        // Ensure minimum rate
        BigDecimal minimumRate = BigDecimal.valueOf(10.00);
        return rate.max(minimumRate);
    }
}
package com.ski.shop.inventory.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Equipment entity representing ski equipment in inventory.
 * This is the main aggregate root for inventory management.
 */
@Entity
@Table(name = "equipment")
public class Equipment extends PanacheEntity {

    @NotNull
    @Column(name = "product_id", nullable = false, unique = true)
    public UUID productId;

    @Column(name = "cached_sku", length = 100)
    public String cachedSku;

    @Column(name = "cached_name", length = 255)
    public String cachedName;

    @Column(name = "cached_category", length = 100)
    public String cachedCategory;

    @Column(name = "cached_brand", length = 100)
    public String cachedBrand;

    @Column(name = "cached_equipment_type", length = 50)
    public String cachedEquipmentType;

    @Column(name = "cached_base_price", precision = 10, scale = 2)
    public BigDecimal cachedBasePrice;

    @NotNull
    @Column(name = "daily_rate", precision = 10, scale = 2, nullable = false)
    public BigDecimal dailyRate;

    @NotNull
    @Column(name = "is_rental_available", nullable = false)
    public Boolean isRentalAvailable = true;

    @NotNull
    @Column(name = "warehouse_id", length = 50, nullable = false)
    public String warehouseId;

    @NotNull
    @PositiveOrZero
    @Column(name = "available_quantity", nullable = false)
    public Integer availableQuantity = 0;

    @NotNull
    @PositiveOrZero
    @Column(name = "reserved_quantity", nullable = false)
    public Integer reservedQuantity = 0;

    @Column(name = "cache_updated_at")
    public LocalDateTime cacheUpdatedAt;

    @NotNull
    @Column(name = "is_active", nullable = false)
    public Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Version
    @Column(name = "version_field")
    public Long version;

    // Constructors
    public Equipment() {}

    public Equipment(UUID productId, String warehouseId, BigDecimal dailyRate) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.dailyRate = dailyRate;
        this.availableQuantity = 0;
        this.reservedQuantity = 0;
        this.isRentalAvailable = true;
        this.isActive = true;
    }

    // Business methods
    public Integer getTotalQuantity() {
        return availableQuantity + reservedQuantity;
    }

    public boolean hasAvailableStock(Integer requiredQuantity) {
        return isActive && isRentalAvailable && availableQuantity >= requiredQuantity;
    }

    public void updateCachedProductInfo(String sku, String name, String category, 
                                       String brand, String equipmentType, BigDecimal basePrice) {
        this.cachedSku = sku;
        this.cachedName = name;
        this.cachedCategory = category;
        this.cachedBrand = brand;
        this.cachedEquipmentType = equipmentType;
        this.cachedBasePrice = basePrice;
        this.cacheUpdatedAt = LocalDateTime.now();
    }

    public void addStock(Integer quantity) {
        if (quantity > 0) {
            this.availableQuantity += quantity;
        }
    }

    public void removeStock(Integer quantity) {
        if (quantity > 0 && this.availableQuantity >= quantity) {
            this.availableQuantity -= quantity;
        }
    }

    public void reserveStock(Integer quantity) {
        if (quantity > 0 && this.availableQuantity >= quantity) {
            this.availableQuantity -= quantity;
            this.reservedQuantity += quantity;
        }
    }

    public void releaseReservedStock(Integer quantity) {
        if (quantity > 0 && this.reservedQuantity >= quantity) {
            this.reservedQuantity -= quantity;
            this.availableQuantity += quantity;
        }
    }

    public void deactivate() {
        this.isActive = false;
        this.isRentalAvailable = false;
    }

    // Static finder methods for Panache
    public static Equipment findByProductId(UUID productId) {
        return find("productId", productId).firstResult();
    }

    public static Equipment findByProductIdForUpdate(UUID productId) {
        return find("productId = ?1 and isActive = true", productId).firstResult();
    }

    @Override
    public String toString() {
        return "Equipment{" +
                "id=" + id +
                ", productId=" + productId +
                ", cachedSku='" + cachedSku + '\'' +
                ", cachedName='" + cachedName + '\'' +
                ", warehouseId='" + warehouseId + '\'' +
                ", availableQuantity=" + availableQuantity +
                ", reservedQuantity=" + reservedQuantity +
                ", isActive=" + isActive +
                '}';
    }
}
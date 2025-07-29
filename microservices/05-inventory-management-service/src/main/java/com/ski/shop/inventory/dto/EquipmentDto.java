package com.ski.shop.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Equipment information
 */
public class EquipmentDto {
    
    @JsonProperty("productId")
    public UUID productId;
    
    @JsonProperty("sku")
    public String sku;
    
    @JsonProperty("name")
    public String name;
    
    @JsonProperty("category")
    public String category;
    
    @JsonProperty("brand")
    public String brand;
    
    @JsonProperty("equipmentType")
    public String equipmentType;
    
    @JsonProperty("basePrice")
    public BigDecimal basePrice;
    
    @JsonProperty("dailyRate")
    public BigDecimal dailyRate;
    
    @JsonProperty("isRentalAvailable")
    public Boolean isRentalAvailable;
    
    @JsonProperty("warehouseId")
    public String warehouseId;
    
    @JsonProperty("availableQuantity")
    public Integer availableQuantity;
    
    @JsonProperty("reservedQuantity")
    public Integer reservedQuantity;
    
    @JsonProperty("totalQuantity")
    public Integer totalQuantity;
    
    @JsonProperty("isActive")
    public Boolean isActive;
    
    @JsonProperty("lastUpdated")
    public LocalDateTime lastUpdated;

    // Constructors
    public EquipmentDto() {}

    public EquipmentDto(UUID productId, String sku, String name, String category, 
                        String brand, String equipmentType, BigDecimal basePrice, 
                        BigDecimal dailyRate, Boolean isRentalAvailable, String warehouseId, 
                        Integer availableQuantity, Integer reservedQuantity, 
                        Boolean isActive, LocalDateTime lastUpdated) {
        this.productId = productId;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.equipmentType = equipmentType;
        this.basePrice = basePrice;
        this.dailyRate = dailyRate;
        this.isRentalAvailable = isRentalAvailable;
        this.warehouseId = warehouseId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.totalQuantity = availableQuantity + reservedQuantity;
        this.isActive = isActive;
        this.lastUpdated = lastUpdated;
    }
}
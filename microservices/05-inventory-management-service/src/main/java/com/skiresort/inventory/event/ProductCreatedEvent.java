package com.skiresort.inventory.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 商品作成イベント
 */
public class ProductCreatedEvent extends ProductEvent {
    
    public UUID productId;
    public String sku;
    public String name;
    public String categoryName;
    public String brandName;
    public String equipmentType; // SKI_BOARD, BOOT, HELMET, etc.
    public String sizeRange;
    public String difficultyLevel;
    public BigDecimal basePrice;
    public String description;
    public String imageUrl;
    public boolean isRentalAvailable;
    public boolean isActive;
    
    public ProductCreatedEvent() {
        super();
    }
    
    // Getters and setters
    public UUID getProductId() {
        return productId;
    }
    
    public void setProductId(UUID productId) {
        this.productId = productId;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public String getBrandName() {
        return brandName;
    }
    
    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }
    
    public String getEquipmentType() {
        return equipmentType;
    }
    
    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }
    
    public String getSizeRange() {
        return sizeRange;
    }
    
    public void setSizeRange(String sizeRange) {
        this.sizeRange = sizeRange;
    }
    
    public String getDifficultyLevel() {
        return difficultyLevel;
    }
    
    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }
    
    public BigDecimal getBasePrice() {
        return basePrice;
    }
    
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public boolean isRentalAvailable() {
        return isRentalAvailable;
    }
    
    public void setRentalAvailable(boolean rentalAvailable) {
        isRentalAvailable = rentalAvailable;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
}
package com.skiresort.inventory.event;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * 商品更新イベント
 */
public class ProductUpdatedEvent extends ProductEvent {
    
    public UUID productId;
    public String sku;
    public String name;
    public String categoryName;
    public String brandName;
    public BigDecimal basePrice;
    public String description;
    public String imageUrl;
    public boolean isActive;
    public Map<String, Object> changedFields; // 変更されたフィールドのみ
    
    public ProductUpdatedEvent() {
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
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public Map<String, Object> getChangedFields() {
        return changedFields;
    }
    
    public void setChangedFields(Map<String, Object> changedFields) {
        this.changedFields = changedFields;
    }
}
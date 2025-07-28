package com.ski.shop.catalog.event;

import com.ski.shop.catalog.domain.Product;

import java.math.BigDecimal;
import java.util.HashMap;
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
    
    public ProductUpdatedEvent(Product oldProduct, Product newProduct) {
        super("PRODUCT_UPDATED", newProduct.id.toString(), 1);
        this.productId = newProduct.id;
        this.sku = newProduct.sku;
        this.name = newProduct.name;
        this.categoryName = newProduct.category.name;
        this.brandName = newProduct.brand.name;
        this.basePrice = newProduct.basePrice;
        this.description = newProduct.description;
        this.imageUrl = extractMainImageUrl(newProduct);
        this.isActive = newProduct.isActive;
        this.changedFields = detectChanges(oldProduct, newProduct);
    }
    
    private String extractMainImageUrl(Product product) {
        if (product.images != null && !product.images.isEmpty()) {
            return product.images.stream()
                    .filter(img -> img.isPrimary)
                    .findFirst()
                    .map(img -> img.imageUrl)
                    .orElse(product.images.get(0).imageUrl);
        }
        return "/images/default-product.jpg";
    }
    
    private Map<String, Object> detectChanges(Product oldProduct, Product newProduct) {
        Map<String, Object> changes = new HashMap<>();
        
        if (!oldProduct.name.equals(newProduct.name)) {
            changes.put("name", newProduct.name);
        }
        if (!oldProduct.description.equals(newProduct.description)) {
            changes.put("description", newProduct.description);
        }
        if (oldProduct.basePrice.compareTo(newProduct.basePrice) != 0) {
            changes.put("basePrice", newProduct.basePrice);
        }
        if (oldProduct.isActive != newProduct.isActive) {
            changes.put("isActive", newProduct.isActive);
        }
        if (!oldProduct.category.id.equals(newProduct.category.id)) {
            changes.put("categoryName", newProduct.category.name);
        }
        if (!oldProduct.brand.id.equals(newProduct.brand.id)) {
            changes.put("brandName", newProduct.brand.name);
        }
        
        return changes;
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
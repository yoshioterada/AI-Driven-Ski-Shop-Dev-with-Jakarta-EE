package com.ski.shop.catalog.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商品削除イベント
 */
public class ProductDeletedEvent extends ProductEvent {
    
    public UUID productId;
    public String sku;
    public LocalDateTime deletedAt;
    
    public ProductDeletedEvent() {
        super();
    }
    
    public ProductDeletedEvent(UUID productId, String sku) {
        super("PRODUCT_DELETED", productId.toString(), 1);
        this.productId = productId;
        this.sku = sku;
        this.deletedAt = LocalDateTime.now();
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
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
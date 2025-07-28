package com.skiresort.inventory.event;

import java.util.UUID;

/**
 * 商品無効化イベント
 */
public class ProductDeactivatedEvent extends ProductEvent {
    
    public UUID productId;
    public String sku;
    
    public ProductDeactivatedEvent() {
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
}
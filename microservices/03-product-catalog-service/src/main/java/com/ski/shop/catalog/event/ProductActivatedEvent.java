package com.ski.shop.catalog.event;

import java.util.UUID;

/**
 * 商品有効化イベント
 */
public class ProductActivatedEvent extends ProductEvent {
    
    public UUID productId;
    public String sku;
    
    public ProductActivatedEvent() {
        super();
    }
    
    public ProductActivatedEvent(UUID productId, String sku) {
        super("PRODUCT_ACTIVATED", productId.toString(), 1);
        this.productId = productId;
        this.sku = sku;
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
package com.ski.shop.inventory.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event classes for Product Catalog integration
 */
public class ProductEvents {

    public static class ProductCreatedEvent {
        @JsonProperty("eventId")
        public UUID eventId;
        
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
        
        @JsonProperty("timestamp")
        public LocalDateTime timestamp;

        public ProductCreatedEvent() {}
    }

    public static class ProductUpdatedEvent {
        @JsonProperty("eventId")
        public UUID eventId;
        
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
        
        @JsonProperty("timestamp")
        public LocalDateTime timestamp;

        public ProductUpdatedEvent() {}
    }

    public static class ProductDeletedEvent {
        @JsonProperty("eventId")
        public UUID eventId;
        
        @JsonProperty("productId")
        public UUID productId;
        
        @JsonProperty("timestamp")
        public LocalDateTime timestamp;

        public ProductDeletedEvent() {}
    }

    public static class ProductPriceChangedEvent {
        @JsonProperty("eventId")
        public UUID eventId;
        
        @JsonProperty("productId")
        public UUID productId;
        
        @JsonProperty("oldPrice")
        public BigDecimal oldPrice;
        
        @JsonProperty("newPrice")
        public BigDecimal newPrice;
        
        @JsonProperty("timestamp")
        public LocalDateTime timestamp;

        public ProductPriceChangedEvent() {}
    }
}
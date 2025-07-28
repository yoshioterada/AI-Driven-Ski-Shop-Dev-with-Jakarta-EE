package com.skiresort.inventory.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商品イベントの基底クラス（インベントリサービス用）
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProductCreatedEvent.class, name = "PRODUCT_CREATED"),
    @JsonSubTypes.Type(value = ProductUpdatedEvent.class, name = "PRODUCT_UPDATED"),
    @JsonSubTypes.Type(value = ProductDeletedEvent.class, name = "PRODUCT_DELETED"),
    @JsonSubTypes.Type(value = ProductActivatedEvent.class, name = "PRODUCT_ACTIVATED"),
    @JsonSubTypes.Type(value = ProductDeactivatedEvent.class, name = "PRODUCT_DEACTIVATED")
})
public abstract class ProductEvent {
    
    public String eventId = UUID.randomUUID().toString();
    public String eventType;
    public LocalDateTime timestamp = LocalDateTime.now();
    public String aggregateId; // Product ID
    public int version; // Event version for ordering
    
    public ProductEvent() {
    }
    
    public ProductEvent(String eventType, String aggregateId, int version) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.version = version;
    }
    
    // Getters and setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    @Override
    public String toString() {
        return "ProductEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                ", aggregateId='" + aggregateId + '\'' +
                ", version=" + version +
                '}';
    }
}
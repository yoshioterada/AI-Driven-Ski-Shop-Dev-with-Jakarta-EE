package com.ski.shop.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for availability check response
 */
public class AvailabilityResponse {
    
    @JsonProperty("productId")
    public UUID productId;
    
    @JsonProperty("totalQuantity")
    public Integer totalQuantity;
    
    @JsonProperty("availableQuantity")
    public Integer availableQuantity;
    
    @JsonProperty("reservedQuantity")
    public Integer reservedQuantity;
    
    @JsonProperty("maintenanceQuantity")
    public Integer maintenanceQuantity;
    
    @JsonProperty("isAvailable")
    public Boolean isAvailable;
    
    @JsonProperty("availabilityByDate")
    public List<DateAvailability> availabilityByDate;
    
    @JsonProperty("nextAvailableDate")
    public LocalDateTime nextAvailableDate;
    
    @JsonProperty("warehouseInfo")
    public WarehouseInfo warehouseInfo;

    // Constructors
    public AvailabilityResponse() {}

    public AvailabilityResponse(UUID productId, Integer totalQuantity, Integer availableQuantity, 
                               Integer reservedQuantity, Integer maintenanceQuantity, 
                               Boolean isAvailable) {
        this.productId = productId;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.maintenanceQuantity = maintenanceQuantity;
        this.isAvailable = isAvailable;
    }

    public static class DateAvailability {
        @JsonProperty("date")
        public LocalDateTime date;
        
        @JsonProperty("availableQuantity")
        public Integer availableQuantity;

        public DateAvailability() {}

        public DateAvailability(LocalDateTime date, Integer availableQuantity) {
            this.date = date;
            this.availableQuantity = availableQuantity;
        }
    }

    public static class WarehouseInfo {
        @JsonProperty("warehouseId")
        public String warehouseId;
        
        @JsonProperty("warehouseName")
        public String warehouseName;

        public WarehouseInfo() {}

        public WarehouseInfo(String warehouseId, String warehouseName) {
            this.warehouseId = warehouseId;
            this.warehouseName = warehouseName;
        }
    }
}
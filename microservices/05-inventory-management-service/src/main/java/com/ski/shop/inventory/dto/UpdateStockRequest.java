package com.ski.shop.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO for updating stock levels
 */
public class UpdateStockRequest {
    
    @NotNull
    @Positive
    @JsonProperty("quantity")
    public Integer quantity;
    
    @NotNull
    @JsonProperty("reason")
    public String reason;
    
    @JsonProperty("warehouseId")
    public String warehouseId;
    
    @JsonProperty("unitCost")
    public BigDecimal unitCost;
    
    @JsonProperty("supplier")
    public String supplier;
    
    @JsonProperty("notes")
    public String notes;

    // Constructors
    public UpdateStockRequest() {}

    public UpdateStockRequest(Integer quantity, String reason) {
        this.quantity = quantity;
        this.reason = reason;
    }
}
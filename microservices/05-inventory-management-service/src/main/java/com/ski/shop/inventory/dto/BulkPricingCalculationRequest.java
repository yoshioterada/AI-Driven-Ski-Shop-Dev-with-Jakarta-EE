package com.ski.shop.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request for bulk pricing calculation
 */
public class BulkPricingCalculationRequest {

    @NotEmpty
    @Valid
    @JsonProperty("pricing_requests")
    public List<PricingCalculationRequest> pricingRequests;

    @JsonProperty("customer_id")
    public String customerId;

    @JsonProperty("apply_bulk_discount")
    public Boolean applyBulkDiscount = false;

    // Constructors
    public BulkPricingCalculationRequest() {}

    public BulkPricingCalculationRequest(List<PricingCalculationRequest> pricingRequests) {
        this.pricingRequests = pricingRequests;
    }

    public BulkPricingCalculationRequest(List<PricingCalculationRequest> pricingRequests, 
                                       String customerId, Boolean applyBulkDiscount) {
        this.pricingRequests = pricingRequests;
        this.customerId = customerId;
        this.applyBulkDiscount = applyBulkDiscount;
    }
}
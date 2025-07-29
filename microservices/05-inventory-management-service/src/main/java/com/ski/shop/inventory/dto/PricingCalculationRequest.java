package com.ski.shop.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.FutureOrPresent;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request for calculating rental pricing
 */
public class PricingCalculationRequest {

    @NotNull
    @JsonProperty("product_id")
    public UUID productId;

    @NotNull
    @Positive
    @JsonProperty("quantity")
    public Integer quantity;

    @NotNull
    @FutureOrPresent
    @JsonProperty("start_date")
    public LocalDate startDate;

    @NotNull
    @FutureOrPresent
    @JsonProperty("end_date")
    public LocalDate endDate;

    @JsonProperty("customer_id")
    public String customerId;

    @JsonProperty("discount_codes")
    public List<String> discountCodes;

    @JsonProperty("customer_tier")
    public String customerTier; // STANDARD, PREMIUM, VIP

    // Constructors
    public PricingCalculationRequest() {}

    public PricingCalculationRequest(UUID productId, Integer quantity, 
                                   LocalDate startDate, LocalDate endDate) {
        this.productId = productId;
        this.quantity = quantity;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Validation helpers
    public boolean isValidDateRange() {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    public int getRentalDays() {
        if (!isValidDateRange()) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
package com.ski.shop.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response for pricing calculation
 */
public class PricingCalculationResponse {

    @JsonProperty("product_id")
    public UUID productId;

    @JsonProperty("quantity")
    public Integer quantity;

    @JsonProperty("rental_days")
    public Integer rentalDays;

    @JsonProperty("base_daily_rate")
    public BigDecimal baseDailyRate;

    @JsonProperty("seasonal_multiplier")
    public BigDecimal seasonalMultiplier;

    @JsonProperty("base_total")
    public BigDecimal baseTotal;

    @JsonProperty("discounts_applied")
    public List<DiscountInfo> discountsApplied;

    @JsonProperty("total_discount_amount")
    public BigDecimal totalDiscountAmount;

    @JsonProperty("final_total")
    public BigDecimal finalTotal;

    @JsonProperty("tax_amount")
    public BigDecimal taxAmount;

    @JsonProperty("grand_total")
    public BigDecimal grandTotal;

    @JsonProperty("start_date")
    public LocalDate startDate;

    @JsonProperty("end_date")
    public LocalDate endDate;

    @JsonProperty("calculation_timestamp")
    public java.time.LocalDateTime calculationTimestamp;

    // Constructors
    public PricingCalculationResponse() {}

    /**
     * Information about applied discounts
     */
    public static class DiscountInfo {
        @JsonProperty("code")
        public String code;

        @JsonProperty("type")
        public String type; // PERCENTAGE, FIXED_AMOUNT, CUSTOMER_TIER

        @JsonProperty("description")
        public String description;

        @JsonProperty("discount_amount")
        public BigDecimal discountAmount;

        public DiscountInfo() {}

        public DiscountInfo(String code, String type, String description, BigDecimal discountAmount) {
            this.code = code;
            this.type = type;
            this.description = description;
            this.discountAmount = discountAmount;
        }
    }
}
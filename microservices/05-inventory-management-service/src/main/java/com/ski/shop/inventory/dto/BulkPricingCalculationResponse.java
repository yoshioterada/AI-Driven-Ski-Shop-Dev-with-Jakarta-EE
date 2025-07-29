package com.ski.shop.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response for bulk pricing calculation
 */
public class BulkPricingCalculationResponse {

    @JsonProperty("individual_calculations")
    public List<PricingCalculationResponse> individualCalculations;

    @JsonProperty("bulk_discount_applied")
    public Boolean bulkDiscountApplied = false;

    @JsonProperty("bulk_discount_amount")
    public BigDecimal bulkDiscountAmount = BigDecimal.ZERO;

    @JsonProperty("subtotal")
    public BigDecimal subtotal;

    @JsonProperty("total_discount_amount")
    public BigDecimal totalDiscountAmount;

    @JsonProperty("total_tax_amount")
    public BigDecimal totalTaxAmount;

    @JsonProperty("grand_total")
    public BigDecimal grandTotal;

    @JsonProperty("calculation_timestamp")
    public java.time.LocalDateTime calculationTimestamp;

    // Constructors
    public BulkPricingCalculationResponse() {}

    public BulkPricingCalculationResponse(List<PricingCalculationResponse> individualCalculations) {
        this.individualCalculations = individualCalculations;
        this.calculationTimestamp = java.time.LocalDateTime.now();
        calculateTotals();
    }

    private void calculateTotals() {
        if (individualCalculations == null || individualCalculations.isEmpty()) {
            this.subtotal = BigDecimal.ZERO;
            this.totalDiscountAmount = BigDecimal.ZERO;
            this.totalTaxAmount = BigDecimal.ZERO;
            this.grandTotal = BigDecimal.ZERO;
            return;
        }

        this.subtotal = individualCalculations.stream()
                .map(calc -> calc.finalTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalDiscountAmount = individualCalculations.stream()
                .map(calc -> calc.totalDiscountAmount != null ? calc.totalDiscountAmount : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalTaxAmount = individualCalculations.stream()
                .map(calc -> calc.taxAmount != null ? calc.taxAmount : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.grandTotal = subtotal
                .add(totalTaxAmount)
                .subtract(bulkDiscountAmount != null ? bulkDiscountAmount : BigDecimal.ZERO);
    }

    public void applyBulkDiscount(BigDecimal discountAmount) {
        this.bulkDiscountApplied = true;
        this.bulkDiscountAmount = discountAmount;
        calculateTotals(); // Recalculate with bulk discount
    }
}
package com.ski.shop.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response for equipment pricing rates
 */
public class EquipmentPricingRatesResponse {

    @JsonProperty("product_id")
    public UUID productId;

    @JsonProperty("equipment_name")
    public String equipmentName;

    @JsonProperty("equipment_type")
    public String equipmentType;

    @JsonProperty("base_daily_rate")
    public BigDecimal baseDailyRate;

    @JsonProperty("weekend_multiplier")
    public BigDecimal weekendMultiplier;

    @JsonProperty("peak_season_multiplier")
    public BigDecimal peakSeasonMultiplier;

    @JsonProperty("off_season_multiplier")
    public BigDecimal offSeasonMultiplier;

    @JsonProperty("minimum_rental_days")
    public Integer minimumRentalDays = 1;

    @JsonProperty("maximum_rental_days")
    public Integer maximumRentalDays = 30;

    @JsonProperty("available_quantity")
    public Integer availableQuantity;

    @JsonProperty("current_season")
    public String currentSeason;

    @JsonProperty("current_multiplier")
    public BigDecimal currentMultiplier;

    @JsonProperty("effective_daily_rate")
    public BigDecimal effectiveDailyRate;

    @JsonProperty("tax_rate")
    public BigDecimal taxRate = new BigDecimal("0.10"); // 10% default tax

    @JsonProperty("last_updated")
    public LocalDate lastUpdated;

    // Constructors
    public EquipmentPricingRatesResponse() {}

    public EquipmentPricingRatesResponse(UUID productId, String equipmentName, 
                                       String equipmentType, BigDecimal baseDailyRate) {
        this.productId = productId;
        this.equipmentName = equipmentName;
        this.equipmentType = equipmentType;
        this.baseDailyRate = baseDailyRate;
        this.lastUpdated = LocalDate.now();
        
        // Set default multipliers
        this.weekendMultiplier = new BigDecimal("1.2");
        this.peakSeasonMultiplier = new BigDecimal("1.5");
        this.offSeasonMultiplier = new BigDecimal("0.8");
    }

    public void calculateEffectiveRate() {
        if (baseDailyRate != null && currentMultiplier != null) {
            this.effectiveDailyRate = baseDailyRate.multiply(currentMultiplier);
        }
    }
}
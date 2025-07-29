package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for calculating rental pricing with seasonal adjustments and discounts
 */
@ApplicationScoped
public class PricingCalculationService {

    private static final Logger LOG = Logger.getLogger(PricingCalculationService.class);

    // Seasonal pricing configuration
    private static final BigDecimal PEAK_SEASON_MULTIPLIER = new BigDecimal("1.5");
    private static final BigDecimal OFF_SEASON_MULTIPLIER = new BigDecimal("0.8");
    private static final BigDecimal STANDARD_MULTIPLIER = new BigDecimal("1.0");
    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.2");

    // Tax rate
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10%

    // Customer tier discounts
    private static final BigDecimal PREMIUM_DISCOUNT = new BigDecimal("0.10"); // 10%
    private static final BigDecimal VIP_DISCOUNT = new BigDecimal("0.15"); // 15%

    // Bulk discount thresholds
    private static final int BULK_DISCOUNT_THRESHOLD = 5;
    private static final BigDecimal BULK_DISCOUNT_RATE = new BigDecimal("0.05"); // 5%

    @Inject
    EquipmentRepository equipmentRepository;

    /**
     * Calculate pricing for a single rental request
     */
    public PricingCalculationResponse calculatePricing(PricingCalculationRequest request) {
        LOG.debugf("Calculating pricing for product %s, quantity %d, days %d", 
                  request.productId, request.quantity, request.getRentalDays());

        // Validate request
        if (!request.isValidDateRange()) {
            throw new IllegalArgumentException("Invalid date range provided");
        }

        // Find equipment
        Equipment equipment = Equipment.findByProductId(request.productId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found for product ID: " + request.productId);
        }

        // Check availability
        if (!equipment.hasAvailableStock(request.quantity)) {
            throw new IllegalArgumentException("Insufficient stock available");
        }

        // Create response
        PricingCalculationResponse response = new PricingCalculationResponse();
        response.productId = request.productId;
        response.quantity = request.quantity;
        response.rentalDays = request.getRentalDays();
        response.startDate = request.startDate;
        response.endDate = request.endDate;
        response.calculationTimestamp = LocalDateTime.now();

        // Base pricing calculation
        response.baseDailyRate = equipment.dailyRate;
        response.seasonalMultiplier = calculateSeasonalMultiplier(request.startDate, request.endDate);
        
        BigDecimal adjustedDailyRate = response.baseDailyRate.multiply(response.seasonalMultiplier);
        response.baseTotal = adjustedDailyRate
                .multiply(BigDecimal.valueOf(request.quantity))
                .multiply(BigDecimal.valueOf(request.getRentalDays()));

        // Apply discounts
        response.discountsApplied = new ArrayList<>();
        response.totalDiscountAmount = BigDecimal.ZERO;

        // Customer tier discount
        if (request.customerTier != null) {
            BigDecimal tierDiscount = calculateCustomerTierDiscount(request.customerTier, response.baseTotal);
            if (tierDiscount.compareTo(BigDecimal.ZERO) > 0) {
                response.discountsApplied.add(new PricingCalculationResponse.DiscountInfo(
                        request.customerTier, "CUSTOMER_TIER", 
                        "Customer tier discount", tierDiscount));
                response.totalDiscountAmount = response.totalDiscountAmount.add(tierDiscount);
            }
        }

        // Discount codes (simplified implementation)
        if (request.discountCodes != null) {
            for (String code : request.discountCodes) {
                BigDecimal codeDiscount = calculateDiscountCodeDiscount(code, response.baseTotal);
                if (codeDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    response.discountsApplied.add(new PricingCalculationResponse.DiscountInfo(
                            code, "DISCOUNT_CODE", 
                            "Promotional discount", codeDiscount));
                    response.totalDiscountAmount = response.totalDiscountAmount.add(codeDiscount);
                }
            }
        }

        // Calculate final amounts
        response.finalTotal = response.baseTotal.subtract(response.totalDiscountAmount);
        response.taxAmount = response.finalTotal.multiply(TAX_RATE);
        response.grandTotal = response.finalTotal.add(response.taxAmount);

        // Round to 2 decimal places
        response.baseTotal = response.baseTotal.setScale(2, RoundingMode.HALF_UP);
        response.totalDiscountAmount = response.totalDiscountAmount.setScale(2, RoundingMode.HALF_UP);
        response.finalTotal = response.finalTotal.setScale(2, RoundingMode.HALF_UP);
        response.taxAmount = response.taxAmount.setScale(2, RoundingMode.HALF_UP);
        response.grandTotal = response.grandTotal.setScale(2, RoundingMode.HALF_UP);

        LOG.debugf("Pricing calculation completed: base=%s, final=%s, tax=%s, grand=%s", 
                  response.baseTotal, response.finalTotal, response.taxAmount, response.grandTotal);

        return response;
    }

    /**
     * Calculate bulk pricing for multiple rental requests
     */
    public BulkPricingCalculationResponse calculateBulkPricing(BulkPricingCalculationRequest request) {
        LOG.debugf("Calculating bulk pricing for %d items", request.pricingRequests.size());

        List<PricingCalculationResponse> individualCalculations = new ArrayList<>();
        
        // Calculate individual pricing
        for (PricingCalculationRequest individualRequest : request.pricingRequests) {
            if (request.customerId != null) {
                individualRequest.customerId = request.customerId;
            }
            individualCalculations.add(calculatePricing(individualRequest));
        }

        BulkPricingCalculationResponse response = new BulkPricingCalculationResponse(individualCalculations);

        // Apply bulk discount if applicable
        if (Boolean.TRUE.equals(request.applyBulkDiscount) && 
            request.pricingRequests.size() >= BULK_DISCOUNT_THRESHOLD) {
            
            BigDecimal bulkDiscountAmount = response.subtotal.multiply(BULK_DISCOUNT_RATE);
            response.applyBulkDiscount(bulkDiscountAmount);
            
            LOG.debugf("Applied bulk discount: %s", bulkDiscountAmount);
        }

        return response;
    }

    /**
     * Get pricing rates for specific equipment
     */
    public EquipmentPricingRatesResponse getEquipmentPricingRates(UUID productId) {
        LOG.debugf("Getting pricing rates for product %s", productId);

        Equipment equipment = Equipment.findByProductId(productId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found for product ID: " + productId);
        }

        EquipmentPricingRatesResponse response = new EquipmentPricingRatesResponse(
                equipment.productId, 
                equipment.cachedName != null ? equipment.cachedName : "Unknown Equipment",
                equipment.cachedEquipmentType != null ? equipment.cachedEquipmentType : "Unknown Type",
                equipment.dailyRate
        );

        response.availableQuantity = equipment.availableQuantity - equipment.pendingReservations;
        
        // Calculate current season and multiplier
        LocalDate today = LocalDate.now();
        response.currentSeason = getCurrentSeason(today);
        response.currentMultiplier = calculateSeasonalMultiplier(today, today);
        response.calculateEffectiveRate();

        return response;
    }

    /**
     * Calculate seasonal multiplier based on date range
     */
    private BigDecimal calculateSeasonalMultiplier(LocalDate startDate, LocalDate endDate) {
        // Peak season: December, January, February (winter skiing season)
        // Off season: May, June, July, August, September
        // Standard season: March, April, October, November

        BigDecimal totalMultiplier = BigDecimal.ZERO;
        int days = 0;

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            BigDecimal dayMultiplier = getDayMultiplier(current);
            totalMultiplier = totalMultiplier.add(dayMultiplier);
            days++;
            current = current.plusDays(1);
        }

        return days > 0 ? totalMultiplier.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP) : STANDARD_MULTIPLIER;
    }

    private BigDecimal getDayMultiplier(LocalDate date) {
        // Base seasonal multiplier
        BigDecimal baseMultiplier = getSeasonalMultiplier(date.getMonth());
        
        // Weekend multiplier
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return baseMultiplier.multiply(WEEKEND_MULTIPLIER);
        }
        
        return baseMultiplier;
    }

    private BigDecimal getSeasonalMultiplier(Month month) {
        return switch (month) {
            case DECEMBER, JANUARY, FEBRUARY -> PEAK_SEASON_MULTIPLIER;
            case MAY, JUNE, JULY, AUGUST, SEPTEMBER -> OFF_SEASON_MULTIPLIER;
            default -> STANDARD_MULTIPLIER;
        };
    }

    private String getCurrentSeason(LocalDate date) {
        Month month = date.getMonth();
        return switch (month) {
            case DECEMBER, JANUARY, FEBRUARY -> "PEAK";
            case MAY, JUNE, JULY, AUGUST, SEPTEMBER -> "OFF_SEASON";
            default -> "STANDARD";
        };
    }

    /**
     * Calculate customer tier discount
     */
    private BigDecimal calculateCustomerTierDiscount(String customerTier, BigDecimal baseAmount) {
        return switch (customerTier.toUpperCase()) {
            case "PREMIUM" -> baseAmount.multiply(PREMIUM_DISCOUNT);
            case "VIP" -> baseAmount.multiply(VIP_DISCOUNT);
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Calculate discount code discount (simplified implementation)
     */
    private BigDecimal calculateDiscountCodeDiscount(String code, BigDecimal baseAmount) {
        // Simplified discount code logic
        return switch (code.toUpperCase()) {
            case "WELCOME10" -> baseAmount.multiply(new BigDecimal("0.10"));
            case "SPRING15" -> baseAmount.multiply(new BigDecimal("0.15"));
            case "EARLY20" -> baseAmount.multiply(new BigDecimal("0.20"));
            default -> BigDecimal.ZERO;
        };
    }
}
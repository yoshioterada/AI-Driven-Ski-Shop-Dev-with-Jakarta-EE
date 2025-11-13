package com.ski.shop.inventory.dto;

import com.ski.shop.inventory.domain.StockReservation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for creating a new stock reservation
 */
public class CreateReservationRequest {

    @NotNull
    public UUID productId;

    @NotNull
    public String customerId;

    @NotNull
    @Positive
    public Integer quantity;

    public StockReservation.ReservationType reservationType = StockReservation.ReservationType.RENTAL;

    // Optional rental period (for future use)
    public LocalDateTime plannedStartDate;
    public LocalDateTime plannedEndDate;

    // Optional timeout override (in minutes from now)
    public Integer timeoutMinutes;

    // Optional business context
    public String referenceNumber;
    public String notes;

    public CreateReservationRequest() {}

    public CreateReservationRequest(UUID productId, String customerId, Integer quantity) {
        this.productId = productId;
        this.customerId = customerId;
        this.quantity = quantity;
    }

    public CreateReservationRequest(UUID productId, String customerId, Integer quantity, 
                                   StockReservation.ReservationType reservationType) {
        this(productId, customerId, quantity);
        this.reservationType = reservationType;
    }
}
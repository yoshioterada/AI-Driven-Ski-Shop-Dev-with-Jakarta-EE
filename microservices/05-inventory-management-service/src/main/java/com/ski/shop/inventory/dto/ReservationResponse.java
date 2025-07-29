package com.ski.shop.inventory.dto;

import com.ski.shop.inventory.domain.StockReservation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for stock reservation information
 */
public class ReservationResponse {

    public UUID reservationId;
    public UUID productId;
    public String customerId;
    public Integer quantity;
    public StockReservation.ReservationStatus status;
    public StockReservation.ReservationType reservationType;

    // Timing information
    public LocalDateTime createdAt;
    public LocalDateTime expiresAt;
    public LocalDateTime confirmedAt;
    public LocalDateTime cancelledAt;
    public Long minutesUntilExpiration;

    // Rental period
    public LocalDateTime plannedStartDate;
    public LocalDateTime plannedEndDate;

    // Business context
    public String referenceNumber;
    public String notes;

    // Equipment information
    public EquipmentInfo equipmentInfo;

    public static class EquipmentInfo {
        public String sku;
        public String name;
        public String category;
        public String brand;
        public String warehouseId;

        public EquipmentInfo() {}

        public EquipmentInfo(String sku, String name, String category, String brand, String warehouseId) {
            this.sku = sku;
            this.name = name;
            this.category = category;
            this.brand = brand;
            this.warehouseId = warehouseId;
        }
    }

    public ReservationResponse() {}

    public ReservationResponse(UUID reservationId, UUID productId, String customerId, 
                              Integer quantity, StockReservation.ReservationStatus status,
                              StockReservation.ReservationType reservationType) {
        this.reservationId = reservationId;
        this.productId = productId;
        this.customerId = customerId;
        this.quantity = quantity;
        this.status = status;
        this.reservationType = reservationType;
    }

    // Static factory method from StockReservation entity
    public static ReservationResponse fromEntity(StockReservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.reservationId = reservation.reservationId;
        response.productId = reservation.productId;
        response.customerId = reservation.customerId;
        response.quantity = reservation.quantity;
        response.status = reservation.status;
        response.reservationType = reservation.reservationType;
        response.createdAt = reservation.createdAt;
        response.expiresAt = reservation.expiresAt;
        response.confirmedAt = reservation.confirmedAt;
        response.cancelledAt = reservation.cancelledAt;
        response.plannedStartDate = reservation.plannedStartDate;
        response.plannedEndDate = reservation.plannedEndDate;
        response.referenceNumber = reservation.referenceNumber;
        response.notes = reservation.notes;
        response.minutesUntilExpiration = reservation.getMinutesUntilExpiration();
        return response;
    }
}
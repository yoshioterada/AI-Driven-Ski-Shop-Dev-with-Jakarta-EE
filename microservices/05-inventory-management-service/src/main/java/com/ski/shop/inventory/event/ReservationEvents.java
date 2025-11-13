package com.ski.shop.inventory.event;

import com.ski.shop.inventory.domain.StockReservation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Events related to stock reservations
 */
public class ReservationEvents {

    /**
     * Base class for reservation events
     */
    public static abstract class ReservationEvent {
        public UUID reservationId;
        public UUID productId;
        public String customerId;
        public Integer quantity;
        public String reservationType;
        public LocalDateTime timestamp = LocalDateTime.now();
        public String eventType;

        protected ReservationEvent(StockReservation reservation, String eventType) {
            this.reservationId = reservation.reservationId;
            this.productId = reservation.productId;
            this.customerId = reservation.customerId;
            this.quantity = reservation.quantity;
            this.reservationType = reservation.reservationType.name();
            this.eventType = eventType;
        }
    }

    /**
     * Event published when a new reservation is created
     */
    public static class StockReservationCreatedEvent extends ReservationEvent {
        public LocalDateTime expiresAt;
        public LocalDateTime plannedStartDate;
        public LocalDateTime plannedEndDate;
        public String referenceNumber;

        public StockReservationCreatedEvent(StockReservation reservation) {
            super(reservation, "STOCK_RESERVATION_CREATED");
            this.expiresAt = reservation.expiresAt;
            this.plannedStartDate = reservation.plannedStartDate;
            this.plannedEndDate = reservation.plannedEndDate;
            this.referenceNumber = reservation.referenceNumber;
        }
    }

    /**
     * Event published when a reservation is confirmed
     */
    public static class StockReservationConfirmedEvent extends ReservationEvent {
        public LocalDateTime confirmedAt;

        public StockReservationConfirmedEvent(StockReservation reservation) {
            super(reservation, "STOCK_RESERVATION_CONFIRMED");
            this.confirmedAt = reservation.confirmedAt;
        }
    }

    /**
     * Event published when a reservation is cancelled
     */
    public static class StockReservationCancelledEvent extends ReservationEvent {
        public LocalDateTime cancelledAt;
        public String cancellationReason;
        public boolean wasConfirmed;

        public StockReservationCancelledEvent(StockReservation reservation, String reason) {
            super(reservation, "STOCK_RESERVATION_CANCELLED");
            this.cancelledAt = reservation.cancelledAt;
            this.cancellationReason = reason;
            this.wasConfirmed = reservation.confirmedAt != null;
        }
    }

    /**
     * Event published when a reservation expires automatically
     */
    public static class StockReservationExpiredEvent extends ReservationEvent {
        public LocalDateTime expiredAt;
        public LocalDateTime originalExpiresAt;

        public StockReservationExpiredEvent(StockReservation reservation) {
            super(reservation, "STOCK_RESERVATION_EXPIRED");
            this.expiredAt = reservation.cancelledAt;
            this.originalExpiresAt = reservation.expiresAt;
        }
    }

    /**
     * Event published when a reservation is about to expire (warning)
     */
    public static class StockReservationExpiringWarningEvent extends ReservationEvent {
        public LocalDateTime expiresAt;
        public long minutesUntilExpiration;

        public StockReservationExpiringWarningEvent(StockReservation reservation) {
            super(reservation, "STOCK_RESERVATION_EXPIRING_WARNING");
            this.expiresAt = reservation.expiresAt;
            this.minutesUntilExpiration = reservation.getMinutesUntilExpiration();
        }
    }
}
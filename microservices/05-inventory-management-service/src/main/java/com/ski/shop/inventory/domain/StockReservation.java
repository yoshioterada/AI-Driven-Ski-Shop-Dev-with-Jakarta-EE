package com.ski.shop.inventory.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * StockReservation entity for managing temporary stock reservations with timeout.
 * Implements Phase 2 reservation system with automatic expiration handling.
 */
@Entity
@Table(name = "stock_reservations")
public class StockReservation extends PanacheEntity {

    /**
     * Reservation status enum
     */
    public enum ReservationStatus {
        PENDING,    // Initial state, can be confirmed or cancelled
        CONFIRMED,  // Reservation confirmed, stock is allocated
        CANCELLED,  // Manually cancelled by user or system
        EXPIRED     // Automatically expired due to timeout
    }

    /**
     * Reservation type enum
     */
    public enum ReservationType {
        RENTAL,     // Equipment rental reservation
        PURCHASE,   // Equipment purchase reservation
        MAINTENANCE // Maintenance reservation (internal use)
    }

    @NotNull
    @Column(name = "reservation_id", nullable = false, unique = true)
    public UUID reservationId;

    @NotNull
    @Column(name = "equipment_id", nullable = false)
    public Long equipmentId;

    @NotNull
    @Column(name = "product_id", nullable = false)
    public UUID productId;

    @NotNull
    @Column(name = "customer_id", nullable = false, length = 255)
    public String customerId;

    @NotNull
    @Positive
    @Column(name = "quantity", nullable = false)
    public Integer quantity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    public ReservationStatus status = ReservationStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", nullable = false, length = 50)
    public ReservationType reservationType = ReservationType.RENTAL;

    // Timeout management
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    public LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    public LocalDateTime cancelledAt;

    // Rental period information
    @Column(name = "planned_start_date")
    public LocalDateTime plannedStartDate;

    @Column(name = "planned_end_date")
    public LocalDateTime plannedEndDate;

    // Business context
    @Column(name = "reference_number", length = 100)
    public String referenceNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    public String notes;

    // Optimistic locking
    @Version
    @Column(name = "version_field")
    public Long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    // Constructors
    public StockReservation() {
        this.reservationId = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
    }

    public StockReservation(Long equipmentId, UUID productId, String customerId, 
                           Integer quantity, LocalDateTime expiresAt) {
        this();
        this.equipmentId = equipmentId;
        this.productId = productId;
        this.customerId = customerId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.PENDING;
        this.reservationType = ReservationType.RENTAL;
    }

    public StockReservation(Long equipmentId, UUID productId, String customerId, 
                           Integer quantity, LocalDateTime expiresAt, ReservationType type) {
        this(equipmentId, productId, customerId, quantity, expiresAt);
        this.reservationType = type;
    }

    // Business methods
    public boolean isActive() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    public boolean isPending() {
        return status == ReservationStatus.PENDING;
    }

    public boolean isExpired() {
        return status == ReservationStatus.EXPIRED || 
               (status == ReservationStatus.PENDING && LocalDateTime.now().isAfter(expiresAt));
    }

    public boolean canBeConfirmed() {
        return status == ReservationStatus.PENDING && !isExpired();
    }

    public boolean canBeCancelled() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    public void confirm() {
        if (!canBeConfirmed()) {
            throw new IllegalStateException("Cannot confirm reservation in current state: " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.cancelledAt = null;
    }

    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Cannot cancel reservation in current state: " + status);
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.confirmedAt = null;
    }

    public void expire() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot expire reservation in current state: " + status);
        }
        this.status = ReservationStatus.EXPIRED;
        this.cancelledAt = LocalDateTime.now();
    }

    public long getMinutesUntilExpiration() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }

    public void extendExpiration(LocalDateTime newExpiresAt) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot extend expired or non-pending reservation");
        }
        if (newExpiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("New expiration time cannot be in the past");
        }
        this.expiresAt = newExpiresAt;
    }

    // Static finder methods for Panache
    public static StockReservation findByReservationId(UUID reservationId) {
        return find("reservationId", reservationId).firstResult();
    }

    public static List<StockReservation> findByCustomerId(String customerId) {
        return find("customerId", customerId).list();
    }

    public static List<StockReservation> findByProductId(UUID productId) {
        return find("productId", productId).list();
    }

    public static List<StockReservation> findByEquipmentId(Long equipmentId) {
        return find("equipmentId", equipmentId).list();
    }

    public static List<StockReservation> findActiveByEquipmentId(Long equipmentId) {
        return find("equipmentId = ?1 and status in ('PENDING', 'CONFIRMED')", equipmentId).list();
    }

    public static List<StockReservation> findExpiredReservations() {
        return find("status = 'PENDING' and expiresAt < ?1", LocalDateTime.now()).list();
    }

    public static List<StockReservation> findReservationsExpiringWithin(int minutes) {
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(minutes);
        return find("status = 'PENDING' and expiresAt between ?1 and ?2", 
                   LocalDateTime.now(), cutoff).list();
    }

    public static List<StockReservation> findByStatus(ReservationStatus status) {
        return find("status", status).list();
    }

    public static List<StockReservation> findByCustomerIdAndStatus(String customerId, ReservationStatus status) {
        return find("customerId = ?1 and status = ?2", customerId, status).list();
    }

    public static long countActiveReservationsForEquipment(Long equipmentId) {
        return count("equipmentId = ?1 and status in ('PENDING', 'CONFIRMED')", equipmentId);
    }

    public static int getTotalReservedQuantityForEquipment(Long equipmentId) {
        return find("equipmentId = ?1 and status in ('PENDING', 'CONFIRMED')", equipmentId)
               .stream()
               .mapToInt(r -> ((StockReservation) r).quantity)
               .sum();
    }

    @Override
    public String toString() {
        return "StockReservation{" +
                "id=" + id +
                ", reservationId=" + reservationId +
                ", equipmentId=" + equipmentId +
                ", productId=" + productId +
                ", customerId='" + customerId + '\'' +
                ", quantity=" + quantity +
                ", status=" + status +
                ", reservationType=" + reservationType +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
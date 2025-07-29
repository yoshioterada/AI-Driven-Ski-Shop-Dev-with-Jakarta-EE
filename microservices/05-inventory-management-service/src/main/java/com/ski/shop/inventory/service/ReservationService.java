package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.domain.StockReservation;
import com.ski.shop.inventory.dto.CreateReservationRequest;
import com.ski.shop.inventory.dto.ExtendReservationRequest;
import com.ski.shop.inventory.dto.ReservationResponse;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing stock reservations with timeout handling
 */
@ApplicationScoped
public class ReservationService {

    // Default reservation timeout in minutes
    private static final int DEFAULT_RESERVATION_TIMEOUT_MINUTES = 30;
    private static final String RESERVATION_CACHE_PREFIX = "reservation:";
    private static final String CUSTOMER_RESERVATIONS_CACHE_PREFIX = "customer_reservations:";
    private static final String EQUIPMENT_RESERVATIONS_CACHE_PREFIX = "equipment_reservations:";
    private static final int CACHE_TTL_SECONDS = 900; // 15 minutes

    @Inject
    EquipmentRepository equipmentRepository;

    @Inject
    ReservationEventService reservationEventService;

    @Inject
    RedisDataSource redisDataSource;

    private ValueCommands<String, String> redisCommands;
    private io.quarkus.redis.datasource.keys.KeyCommands keyCommands;
    private ObjectMapper objectMapper = new ObjectMapper();

    public void init() {
        this.redisCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key();
    }

    /**
     * Create a new stock reservation
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        Log.infof("Creating reservation for product %s, customer %s, quantity %d", 
                 request.productId, request.customerId, request.quantity);

        // Find the equipment
        Optional<Equipment> equipmentOpt = equipmentRepository.findByProductId(request.productId);
        if (equipmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Equipment not found for product ID: " + request.productId);
        }

        Equipment equipment = equipmentOpt.get();
        
        // Check if equipment is active and rental available
        if (!equipment.isActive || !equipment.isRentalAvailable) {
            throw new IllegalStateException("Equipment is not available for reservation");
        }

        // Check availability with atomic operation using optimistic locking
        if (!equipment.hasAvailableStock(request.quantity)) {
            throw new IllegalStateException("Insufficient stock available for reservation. " +
                    "Available: " + (equipment.availableQuantity - equipment.pendingReservations) + 
                    ", Requested: " + request.quantity);
        }

        // Calculate expiration time
        int timeoutMinutes = request.timeoutMinutes != null ? 
                           request.timeoutMinutes : DEFAULT_RESERVATION_TIMEOUT_MINUTES;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);

        // Create the reservation
        StockReservation reservation = new StockReservation(
                equipment.id, 
                request.productId, 
                request.customerId, 
                request.quantity, 
                expiresAt,
                request.reservationType
        );

        // Set optional fields
        reservation.plannedStartDate = request.plannedStartDate;
        reservation.plannedEndDate = request.plannedEndDate;
        reservation.referenceNumber = request.referenceNumber;
        reservation.notes = request.notes;

        // Persist the reservation first
        reservation.persist();

        // Update equipment pending reservations in the same transaction
        equipment.pendingReservations += request.quantity;
        equipment.persist();

        Log.infof("Reservation created successfully: %s", reservation.reservationId);

        // Publish reservation created event
        reservationEventService.publishReservationCreated(reservation);

        // Return response with equipment info
        ReservationResponse response = ReservationResponse.fromEntity(reservation);
        response.equipmentInfo = new ReservationResponse.EquipmentInfo(
                equipment.cachedSku,
                equipment.cachedName,
                equipment.cachedCategory,
                equipment.cachedBrand,
                equipment.warehouseId
        );

        // Invalidate related caches
        invalidateCustomerReservationsCache(request.customerId);
        invalidateEquipmentReservationsCache(request.productId);

        return response;
    }

    /**
     * Get reservation details by reservation ID with caching
     */
    public ReservationResponse getReservation(UUID reservationId) {
        Log.debugf("Getting reservation details for %s", reservationId);

        // Try cache first
        ReservationResponse cached = getCachedReservation(reservationId);
        if (cached != null) {
            return cached;
        }

        StockReservation reservation = StockReservation.findByReservationId(reservationId);
        if (reservation == null) {
            return null;
        }

        ReservationResponse response = ReservationResponse.fromEntity(reservation);
        
        // Add equipment information
        Equipment equipment = equipmentRepository.findById(reservation.equipmentId);
        if (equipment != null) {
            response.equipmentInfo = new ReservationResponse.EquipmentInfo(
                    equipment.cachedSku,
                    equipment.cachedName,
                    equipment.cachedCategory,
                    equipment.cachedBrand,
                    equipment.warehouseId
            );
        }

        // Cache the response
        cacheReservation(response);

        return response;
    }

    /**
     * Confirm a pending reservation
     */
    @Transactional
    public ReservationResponse confirmReservation(UUID reservationId) {
        Log.infof("Confirming reservation %s", reservationId);

        StockReservation reservation = StockReservation.findByReservationId(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }

        if (!reservation.canBeConfirmed()) {
            throw new IllegalStateException("Reservation cannot be confirmed in current state: " + 
                                          reservation.status + " (expired: " + reservation.isExpired() + ")");
        }

        // Get equipment for stock update
        Optional<Equipment> equipmentOpt = equipmentRepository.findByIdForUpdate(reservation.equipmentId);
        if (equipmentOpt.isEmpty()) {
            throw new IllegalStateException("Equipment not found for reservation");
        }

        Equipment equipment = equipmentOpt.get();
        
        // Move stock from available to reserved and reduce pending reservations (atomic operation)
        equipment.reserveStock(reservation.quantity);
        equipment.pendingReservations -= reservation.quantity;
        equipment.persist();

        // Update reservation status
        reservation.confirm();
        reservation.persist();

        Log.infof("Reservation confirmed successfully: %s", reservationId);

        // Publish reservation confirmed event
        reservationEventService.publishReservationConfirmed(reservation);

        // Invalidate caches
        invalidateReservationCache(reservationId);
        invalidateCustomerReservationsCache(reservation.customerId);
        
        // Get equipment to invalidate equipment reservations cache
        Equipment equipmentForCache = equipmentRepository.findById(reservation.equipmentId);
        if (equipmentForCache != null) {
            invalidateEquipmentReservationsCache(equipmentForCache.productId);
        }

        return ReservationResponse.fromEntity(reservation);
    }

    /**
     * Cancel a reservation
     */
    @Transactional
    public ReservationResponse cancelReservation(UUID reservationId, String reason) {
        Log.infof("Cancelling reservation %s, reason: %s", reservationId, reason);

        StockReservation reservation = StockReservation.findByReservationId(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }

        if (!reservation.canBeCancelled()) {
            throw new IllegalStateException("Reservation cannot be cancelled in current state: " + reservation.status);
        }

        boolean wasConfirmed = reservation.status == StockReservation.ReservationStatus.CONFIRMED;
        boolean wasPending = reservation.status == StockReservation.ReservationStatus.PENDING;

        // Handle stock and pending reservations based on current status
        if (wasConfirmed) {
            // If reservation was confirmed, get equipment and release reserved stock back to available
            Optional<Equipment> equipmentOpt = equipmentRepository.findByIdForUpdate(reservation.equipmentId);
            if (equipmentOpt.isPresent()) {
                Equipment equipment = equipmentOpt.get();
                equipment.releaseReservedStock(reservation.quantity);
                equipment.persist();
            }
        } else if (wasPending) {
            // If reservation was pending, just reduce pending reservations count from the current equipment
            Equipment equipment = equipmentRepository.findById(reservation.equipmentId);
            if (equipment != null) {
                equipment.pendingReservations -= reservation.quantity;
                equipment.persist();
            }
        }

        // Update reservation status
        reservation.cancel();
        if (reason != null && !reason.trim().isEmpty()) {
            reservation.notes = (reservation.notes != null ? reservation.notes + "\n" : "") + 
                              "Cancelled: " + reason;
        }
        reservation.persist();

        Log.infof("Reservation cancelled successfully: %s", reservationId);

        // Publish reservation cancelled event
        reservationEventService.publishReservationCancelled(reservation, reason);

        // Invalidate caches
        invalidateReservationCache(reservationId);
        invalidateCustomerReservationsCache(reservation.customerId);
        
        // Get equipment to invalidate equipment reservations cache
        Equipment equipmentForCacheInvalidation = equipmentRepository.findById(reservation.equipmentId);
        if (equipmentForCacheInvalidation != null) {
            invalidateEquipmentReservationsCache(equipmentForCacheInvalidation.productId);
        }

        return ReservationResponse.fromEntity(reservation);
    }

    /**
     * Extend reservation expiration time
     */
    @Transactional
    public ReservationResponse extendReservation(UUID reservationId, ExtendReservationRequest request) {
        Log.infof("Extending reservation %s by %d minutes", reservationId, request.additionalMinutes);

        StockReservation reservation = StockReservation.findByReservationId(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }

        if (reservation.status != StockReservation.ReservationStatus.PENDING) {
            throw new IllegalStateException("Can only extend pending reservations");
        }

        LocalDateTime newExpiresAt = reservation.expiresAt.plusMinutes(request.additionalMinutes);
        reservation.extendExpiration(newExpiresAt);

        if (request.reason != null && !request.reason.trim().isEmpty()) {
            reservation.notes = (reservation.notes != null ? reservation.notes + "\n" : "") + 
                              "Extended: " + request.reason;
        }

        reservation.persist();

        Log.infof("Reservation extended successfully: %s, new expiration: %s", reservationId, newExpiresAt);

        return ReservationResponse.fromEntity(reservation);
    }

    /**
     * Get reservations for a customer with caching
     */
    public List<ReservationResponse> getCustomerReservations(String customerId) {
        Log.debugf("Getting reservations for customer %s", customerId);

        // Try cache first
        List<ReservationResponse> cached = getCachedCustomerReservations(customerId);
        if (cached != null) {
            return cached;
        }

        List<StockReservation> reservations = StockReservation.findByCustomerId(customerId);
        List<ReservationResponse> response = reservations.stream()
                .map(ReservationResponse::fromEntity)
                .collect(Collectors.toList());
        
        // Cache the response
        cacheCustomerReservations(customerId, response);
        
        return response;
    }

    /**
     * Get active reservations for equipment with caching
     */
    public List<ReservationResponse> getEquipmentReservations(UUID productId) {
        Log.debugf("Getting reservations for product %s", productId);

        // Try cache first
        List<ReservationResponse> cached = getCachedEquipmentReservations(productId);
        if (cached != null) {
            return cached;
        }

        List<StockReservation> reservations = StockReservation.findByProductId(productId);
        List<ReservationResponse> response = reservations.stream()
                .map(ReservationResponse::fromEntity)
                .collect(Collectors.toList());
        
        // Cache the response
        cacheEquipmentReservations(productId, response);
        
        return response;
    }

    /**
     * Process expired reservations (called by background scheduler)
     */
    @Transactional
    public int processExpiredReservations() {
        Log.debug("Processing expired reservations");

        List<StockReservation> expiredReservations = StockReservation.findExpiredReservations();
        
        for (StockReservation reservation : expiredReservations) {
            try {
                Log.infof("Expiring reservation %s", reservation.reservationId);
                
                // Update equipment pending reservations for expired reservations
                if (reservation.status == StockReservation.ReservationStatus.PENDING) {
                    Equipment equipment = equipmentRepository.findById(reservation.equipmentId);
                    if (equipment != null) {
                        equipment.pendingReservations -= reservation.quantity;
                        equipment.persist();
                    }
                }
                
                reservation.expire();
                reservation.persist();

                // Publish reservation expired event
                reservationEventService.publishReservationExpired(reservation);
                
            } catch (Exception e) {
                Log.errorf(e, "Failed to expire reservation %s", reservation.reservationId);
            }
        }

        Log.infof("Processed %d expired reservations", expiredReservations.size());
        return expiredReservations.size();
    }

    /**
     * Get reservations expiring soon (for notifications)
     */
    public List<ReservationResponse> getReservationsExpiringSoon(int withinMinutes) {
        List<StockReservation> reservations = StockReservation.findReservationsExpiringWithin(withinMinutes);
        return reservations.stream()
                .map(ReservationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // Cache management methods

    /**
     * Cache reservation data
     */
    private void cacheReservation(ReservationResponse reservation) {
        if (redisCommands == null) {
            init();
        }

        try {
            String cacheKey = RESERVATION_CACHE_PREFIX + reservation.reservationId;
            String serialized = objectMapper.writeValueAsString(reservation);
            redisCommands.setex(cacheKey, CACHE_TTL_SECONDS, serialized);
            Log.debugf("Cached reservation: %s", reservation.reservationId);
        } catch (Exception e) {
            Log.warnf("Failed to cache reservation %s: %s", reservation.reservationId, e.getMessage());
        }
    }

    /**
     * Get reservation from cache
     */
    private ReservationResponse getCachedReservation(UUID reservationId) {
        if (redisCommands == null) {
            init();
        }

        try {
            String cacheKey = RESERVATION_CACHE_PREFIX + reservationId;
            String cached = redisCommands.get(cacheKey);
            if (cached != null) {
                Log.debugf("Cache hit for reservation: %s", reservationId);
                return objectMapper.readValue(cached, ReservationResponse.class);
            }
        } catch (Exception e) {
            Log.warnf("Failed to read cached reservation %s: %s", reservationId, e.getMessage());
        }
        return null;
    }

    /**
     * Invalidate reservation cache
     */
    private void invalidateReservationCache(UUID reservationId) {
        if (redisCommands == null) {
            init();
        }

        try {
            String cacheKey = RESERVATION_CACHE_PREFIX + reservationId;
            keyCommands.del(cacheKey);
            Log.debugf("Invalidated cache for reservation: %s", reservationId);
        } catch (Exception e) {
            Log.warnf("Failed to invalidate cache for reservation %s: %s", reservationId, e.getMessage());
        }
    }

    /**
     * Cache customer reservations list
     */
    private void cacheCustomerReservations(String customerId, List<ReservationResponse> reservations) {
        if (redisCommands == null) {
            init();
        }

        try {
            String cacheKey = CUSTOMER_RESERVATIONS_CACHE_PREFIX + customerId;
            String serialized = objectMapper.writeValueAsString(reservations);
            redisCommands.setex(cacheKey, CACHE_TTL_SECONDS / 2, serialized); // Shorter TTL for lists
            Log.debugf("Cached customer reservations for: %s", customerId);
        } catch (Exception e) {
            Log.warnf("Failed to cache customer reservations for %s: %s", customerId, e.getMessage());
        }
    }

    /**
     * Get customer reservations from cache
     */
    @SuppressWarnings("unchecked")
    private List<ReservationResponse> getCachedCustomerReservations(String customerId) {
        if (redisCommands == null) {
            init();
        }

        try {
            String cacheKey = CUSTOMER_RESERVATIONS_CACHE_PREFIX + customerId;
            String cached = redisCommands.get(cacheKey);
            if (cached != null) {
                Log.debugf("Cache hit for customer reservations: %s", customerId);
                return objectMapper.readValue(cached, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ReservationResponse.class));
            }
        } catch (Exception e) {
            Log.warnf("Failed to read cached customer reservations for %s: %s", customerId, e.getMessage());
        }
        return null;
    }

    /**
     * Invalidate customer reservations cache
     */
    private void invalidateCustomerReservationsCache(String customerId) {
        if (redisCommands == null) {
            init();
        }

        try {
            String cacheKey = CUSTOMER_RESERVATIONS_CACHE_PREFIX + customerId;
            keyCommands.del(cacheKey);
            Log.debugf("Invalidated cache for customer reservations: %s", customerId);
        } catch (Exception e) {
            Log.warnf("Failed to invalidate customer reservations cache for %s: %s", customerId, e.getMessage());
        }
    }

    /**
     * Cache equipment reservations list
     */
    private void cacheEquipmentReservations(UUID productId, List<ReservationResponse> reservations) {
        if (redisCommands == null) {
            init();
        }

        try {
            String cacheKey = EQUIPMENT_RESERVATIONS_CACHE_PREFIX + productId;
            String serialized = objectMapper.writeValueAsString(reservations);
            redisCommands.setex(cacheKey, CACHE_TTL_SECONDS / 2, serialized); // Shorter TTL for lists
            Log.debugf("Cached equipment reservations for: %s", productId);
        } catch (Exception e) {
            Log.warnf("Failed to cache equipment reservations for %s: %s", productId, e.getMessage());
        }
    }

    /**
     * Get equipment reservations from cache
     */
    @SuppressWarnings("unchecked")
    private List<ReservationResponse> getCachedEquipmentReservations(UUID productId) {
        if (redisCommands == null) {
            init();
        }

        try {
            String cacheKey = EQUIPMENT_RESERVATIONS_CACHE_PREFIX + productId;
            String cached = redisCommands.get(cacheKey);
            if (cached != null) {
                Log.debugf("Cache hit for equipment reservations: %s", productId);
                return objectMapper.readValue(cached, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ReservationResponse.class));
            }
        } catch (Exception e) {
            Log.warnf("Failed to read cached equipment reservations for %s: %s", productId, e.getMessage());
        }
        return null;
    }

    /**
     * Invalidate equipment reservations cache
     */
    private void invalidateEquipmentReservationsCache(UUID productId) {
        if (redisCommands == null) {
            init();
        }

        try {
            String cacheKey = EQUIPMENT_RESERVATIONS_CACHE_PREFIX + productId;
            keyCommands.del(cacheKey);
            Log.debugf("Invalidated cache for equipment reservations: %s", productId);
        } catch (Exception e) {
            Log.warnf("Failed to invalidate equipment reservations cache for %s: %s", productId, e.getMessage());
        }
    }
}
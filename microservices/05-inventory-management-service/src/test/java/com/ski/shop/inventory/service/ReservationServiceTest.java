package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.domain.StockReservation;
import com.ski.shop.inventory.dto.CreateReservationRequest;
import com.ski.shop.inventory.dto.ExtendReservationRequest;
import com.ski.shop.inventory.dto.ReservationResponse;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReservationService
 */
@QuarkusTest
@TestProfile(NoKafkaTestProfile.class)
class ReservationServiceTest {

    @Inject
    ReservationService reservationService;

    @Inject
    EquipmentRepository equipmentRepository;

    private UUID testProductId;
    private Equipment testEquipment;
    private final String testCustomerId = "customer-123";

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing test data
        StockReservation.deleteAll();
        Equipment.deleteAll();

        // Create test equipment
        testProductId = UUID.randomUUID();
        testEquipment = new Equipment(testProductId, "WAREHOUSE-1", new BigDecimal("50.00"));
        testEquipment.cachedSku = "TEST-SKI-001";
        testEquipment.cachedName = "Test Ski Equipment";
        testEquipment.cachedCategory = "SKIS";
        testEquipment.cachedBrand = "TestBrand";
        testEquipment.availableQuantity = 10;
        testEquipment.persist();
    }

    @Test
    @TestTransaction
    void testCreateReservation_Success() {
        // Given
        CreateReservationRequest request = new CreateReservationRequest(
                testProductId, testCustomerId, 2);
        request.timeoutMinutes = 60;
        request.notes = "Test reservation";

        // When
        ReservationResponse response = reservationService.createReservation(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.reservationId);
        assertEquals(testProductId, response.productId);
        assertEquals(testCustomerId, response.customerId);
        assertEquals(2, response.quantity);
        assertEquals(StockReservation.ReservationStatus.PENDING, response.status);
        assertNotNull(response.expiresAt);
        assertTrue(response.expiresAt.isAfter(LocalDateTime.now()));
        assertNotNull(response.equipmentInfo);
        assertEquals("TEST-SKI-001", response.equipmentInfo.sku);

        // Verify equipment pending reservations updated
        Equipment updated = Equipment.findByProductId(testProductId);
        assertEquals(2, updated.pendingReservations);
    }

    @Test
    @TestTransaction
    void testCreateReservation_InsufficientStock() {
        // Given
        CreateReservationRequest request = new CreateReservationRequest(
                testProductId, testCustomerId, 15); // More than available

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
                () -> reservationService.createReservation(request));
        
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    @TestTransaction
    void testCreateReservation_EquipmentNotFound() {
        // Given
        UUID nonExistentProductId = UUID.randomUUID();
        CreateReservationRequest request = new CreateReservationRequest(
                nonExistentProductId, testCustomerId, 1);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> reservationService.createReservation(request));
        
        assertTrue(exception.getMessage().contains("Equipment not found"));
    }

    @Test
    @TestTransaction
    void testConfirmReservation_Success() {
        // Given - Create a reservation first
        CreateReservationRequest createRequest = new CreateReservationRequest(
                testProductId, testCustomerId, 3);
        ReservationResponse created = reservationService.createReservation(createRequest);

        // When
        ReservationResponse confirmed = reservationService.confirmReservation(created.reservationId);

        // Then
        assertEquals(StockReservation.ReservationStatus.CONFIRMED, confirmed.status);
        assertNotNull(confirmed.confirmedAt);

        // Verify stock movement
        Equipment updated = Equipment.findByProductId(testProductId);
        assertEquals(7, updated.availableQuantity); // 10 - 3
        assertEquals(3, updated.reservedQuantity);
        assertEquals(0, updated.pendingReservations); // Should be cleared
    }

    @Test
    @TestTransaction
    void testConfirmReservation_NotFound() {
        // Given
        UUID nonExistentReservationId = UUID.randomUUID();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> reservationService.confirmReservation(nonExistentReservationId));
        
        assertTrue(exception.getMessage().contains("Reservation not found"));
    }

    @Test
    @TestTransaction
    void testCancelReservation_Success() {
        // Given - Create a reservation first
        CreateReservationRequest createRequest = new CreateReservationRequest(
                testProductId, testCustomerId, 2);
        ReservationResponse created = reservationService.createReservation(createRequest);

        // When
        ReservationResponse cancelled = reservationService.cancelReservation(
                created.reservationId, "Customer request");

        // Then
        assertEquals(StockReservation.ReservationStatus.CANCELLED, cancelled.status);
        assertNotNull(cancelled.cancelledAt);
        assertTrue(cancelled.notes.contains("Cancelled: Customer request"));

        // Verify pending reservations cleared
        Equipment updated = Equipment.findByProductId(testProductId);
        assertEquals(0, updated.pendingReservations);
    }

    @Test
    @TestTransaction
    void testCancelConfirmedReservation_Success() {
        // Given - Create and confirm a reservation
        CreateReservationRequest createRequest = new CreateReservationRequest(
                testProductId, testCustomerId, 2);
        ReservationResponse created = reservationService.createReservation(createRequest);
        reservationService.confirmReservation(created.reservationId);

        // When
        ReservationResponse cancelled = reservationService.cancelReservation(
                created.reservationId, "Changed plans");

        // Then
        assertEquals(StockReservation.ReservationStatus.CANCELLED, cancelled.status);

        // Verify stock released back to available
        Equipment updated = Equipment.findByProductId(testProductId);
        assertEquals(10, updated.availableQuantity); // Back to original
        assertEquals(0, updated.reservedQuantity);
    }

    @Test
    @TestTransaction
    void testExtendReservation_Success() {
        // Given - Create a reservation first
        CreateReservationRequest createRequest = new CreateReservationRequest(
                testProductId, testCustomerId, 1);
        createRequest.timeoutMinutes = 30;
        ReservationResponse created = reservationService.createReservation(createRequest);
        LocalDateTime originalExpiration = created.expiresAt;

        // When
        ExtendReservationRequest extendRequest = new ExtendReservationRequest(60, "Need more time");
        ReservationResponse extended = reservationService.extendReservation(
                created.reservationId, extendRequest);

        // Then
        assertTrue(extended.expiresAt.isAfter(originalExpiration));
        assertEquals(originalExpiration.plusMinutes(60), extended.expiresAt);
        assertTrue(extended.notes.contains("Extended: Need more time"));
    }

    @Test
    @TestTransaction
    void testGetCustomerReservations() {
        // Given - Create multiple reservations
        CreateReservationRequest request1 = new CreateReservationRequest(
                testProductId, testCustomerId, 1);
        CreateReservationRequest request2 = new CreateReservationRequest(
                testProductId, testCustomerId, 2);
        
        reservationService.createReservation(request1);
        reservationService.createReservation(request2);

        // When
        List<ReservationResponse> reservations = reservationService.getCustomerReservations(testCustomerId);

        // Then
        assertEquals(2, reservations.size());
        assertTrue(reservations.stream().allMatch(r -> r.customerId.equals(testCustomerId)));
    }

    @Test
    @TestTransaction
    void testGetEquipmentReservations() {
        // Given - Create reservations for the equipment
        CreateReservationRequest request = new CreateReservationRequest(
                testProductId, "customer-1", 1);
        reservationService.createReservation(request);

        // When
        List<ReservationResponse> reservations = reservationService.getEquipmentReservations(testProductId);

        // Then
        assertEquals(1, reservations.size());
        assertEquals(testProductId, reservations.get(0).productId);
    }

    @Test
    @TestTransaction
    void testProcessExpiredReservations() {
        // Given - Create a reservation that should be expired
        CreateReservationRequest request = new CreateReservationRequest(
                testProductId, testCustomerId, 2);
        request.timeoutMinutes = 1; // Very short timeout
        ReservationResponse created = reservationService.createReservation(request);

        // Manually set expiration to past
        StockReservation reservation = StockReservation.findByReservationId(created.reservationId);
        reservation.expiresAt = LocalDateTime.now().minusMinutes(5);
        reservation.persist();

        // When
        int expiredCount = reservationService.processExpiredReservations();

        // Then
        assertEquals(1, expiredCount);

        // Verify reservation is expired
        StockReservation updated = StockReservation.findByReservationId(created.reservationId);
        assertEquals(StockReservation.ReservationStatus.EXPIRED, updated.status);

        // Verify pending reservations cleared
        Equipment equipment = Equipment.findByProductId(testProductId);
        assertEquals(0, equipment.pendingReservations);
    }

    @Test
    @TestTransaction
    void testAvailabilityWithPendingReservations() {
        // Given - Equipment with 10 available, create reservation for 8
        CreateReservationRequest request = new CreateReservationRequest(
                testProductId, testCustomerId, 8);
        reservationService.createReservation(request);

        // When - Check availability for 3 more (should fail, only 2 effectively available)
        Equipment equipment = Equipment.findByProductId(testProductId);

        // Then
        assertFalse(equipment.hasAvailableStock(3));
        assertTrue(equipment.hasAvailableStock(2));
    }
}
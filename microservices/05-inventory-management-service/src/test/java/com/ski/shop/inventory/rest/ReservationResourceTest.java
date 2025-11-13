package com.ski.shop.inventory.rest;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.dto.CreateReservationRequest;
import com.ski.shop.inventory.dto.ExtendReservationRequest;
import com.ski.shop.inventory.service.NoKafkaTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for ReservationResource REST endpoints
 */
@QuarkusTest
@TestProfile(NoKafkaTestProfile.class)
class ReservationResourceTest {

    private UUID testProductId;
    private final String testCustomerId = "customer-123";

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing test data - order matters due to foreign keys
        Equipment.getEntityManager().createQuery("DELETE FROM StockReservation").executeUpdate();
        Equipment.deleteAll();

        // Create test equipment
        testProductId = UUID.randomUUID();
        Equipment testEquipment = new Equipment(testProductId, "WAREHOUSE-1", new BigDecimal("50.00"));
        testEquipment.cachedSku = "TEST-SKI-001";
        testEquipment.cachedName = "Test Ski Equipment";
        testEquipment.cachedCategory = "SKIS";
        testEquipment.cachedBrand = "TestBrand";
        testEquipment.availableQuantity = 10;
        testEquipment.persist();
    }

    @Test
    void testCreateReservation_Success() {
        CreateReservationRequest request = new CreateReservationRequest(
                testProductId, testCustomerId, 2);
        request.timeoutMinutes = 60;
        request.notes = "Test reservation";

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/inventory/reservations")
        .then()
                .statusCode(201)
                .body("productId", equalTo(testProductId.toString()))
                .body("customerId", equalTo(testCustomerId))
                .body("quantity", equalTo(2))
                .body("status", equalTo("PENDING"))
                .body("reservationId", notNullValue())
                .body("expiresAt", notNullValue())
                .body("equipmentInfo.sku", equalTo("TEST-SKI-001"));
    }

    @Test
    void testCreateReservation_InsufficientStock() {
        CreateReservationRequest request = new CreateReservationRequest(
                testProductId, testCustomerId, 15); // More than available

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/inventory/reservations")
        .then()
                .statusCode(409)
                .body("errorCode", equalTo("INSUFFICIENT_STOCK"))
                .body("message", containsString("Insufficient stock"));
    }

    @Test
    void testCreateReservation_EquipmentNotFound() {
        UUID nonExistentProductId = UUID.randomUUID();
        CreateReservationRequest request = new CreateReservationRequest(
                nonExistentProductId, testCustomerId, 1);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/inventory/reservations")
        .then()
                .statusCode(400)
                .body("errorCode", equalTo("INVALID_REQUEST"))
                .body("message", containsString("Equipment not found"));
    }

    @Test
    void testReservationLifecycle() {
        // 1. Create reservation
        CreateReservationRequest createRequest = new CreateReservationRequest(
                testProductId, testCustomerId, 3);

        String reservationId = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
        .when()
                .post("/api/v1/inventory/reservations")
        .then()
                .statusCode(201)
                .extract()
                .path("reservationId");

        // 2. Get reservation details
        given()
        .when()
                .get("/api/v1/inventory/reservations/{reservationId}", reservationId)
        .then()
                .statusCode(200)
                .body("reservationId", equalTo(reservationId))
                .body("status", equalTo("PENDING"));

        // 3. Confirm reservation
        given()
        .when()
                .put("/api/v1/inventory/reservations/{reservationId}/confirm", reservationId)
        .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("confirmedAt", notNullValue());

        // 4. Cancel reservation
        given()
                .queryParam("reason", "Changed plans")
        .when()
                .put("/api/v1/inventory/reservations/{reservationId}/cancel", reservationId)
        .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"))
                .body("cancelledAt", notNullValue());
    }

    @Test
    void testExtendReservation() {
        // Create reservation first
        CreateReservationRequest createRequest = new CreateReservationRequest(
                testProductId, testCustomerId, 1);
        createRequest.timeoutMinutes = 30;

        String reservationId = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
        .when()
                .post("/api/v1/inventory/reservations")
        .then()
                .statusCode(201)
                .extract()
                .path("reservationId");

        // Extend reservation
        ExtendReservationRequest extendRequest = new ExtendReservationRequest(60, "Need more time");

        given()
                .contentType(ContentType.JSON)
                .body(extendRequest)
        .when()
                .put("/api/v1/inventory/reservations/{reservationId}/extend", reservationId)
        .then()
                .statusCode(200)
                .body("notes", containsString("Extended: Need more time"));
    }

    @Test
    void testGetCustomerReservations() {
        // Create multiple reservations for the customer
        CreateReservationRequest request1 = new CreateReservationRequest(
                testProductId, testCustomerId, 1);
        CreateReservationRequest request2 = new CreateReservationRequest(
                testProductId, testCustomerId, 2);

        given()
                .contentType(ContentType.JSON)
                .body(request1)
        .when()
                .post("/api/v1/inventory/reservations")
        .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(request2)
        .when()
                .post("/api/v1/inventory/reservations")
        .then()
                .statusCode(201);

        // Get customer reservations
        given()
        .when()
                .get("/api/v1/inventory/reservations/customer/{customerId}", testCustomerId)
        .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].customerId", equalTo(testCustomerId))
                .body("[1].customerId", equalTo(testCustomerId));
    }

    @Test
    void testGetEquipmentReservations() {
        // Create reservation for the equipment
        CreateReservationRequest request = new CreateReservationRequest(
                testProductId, "customer-1", 1);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/inventory/reservations")
        .then()
                .statusCode(201);

        // Get equipment reservations
        given()
        .when()
                .get("/api/v1/inventory/reservations/equipment/{productId}", testProductId)
        .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].productId", equalTo(testProductId.toString()));
    }

    @Test
    void testGetReservation_NotFound() {
        UUID nonExistentReservationId = UUID.randomUUID();

        given()
        .when()
                .get("/api/v1/inventory/reservations/{reservationId}", nonExistentReservationId)
        .then()
                .statusCode(404)
                .body("errorCode", equalTo("NOT_FOUND"));
    }

    @Test
    void testConfirmReservation_NotFound() {
        UUID nonExistentReservationId = UUID.randomUUID();

        given()
        .when()
                .put("/api/v1/inventory/reservations/{reservationId}/confirm", nonExistentReservationId)
        .then()
                .statusCode(404)
                .body("errorCode", equalTo("NOT_FOUND"));
    }

    @Test
    void testValidationErrors() {
        // Test with invalid request (missing required fields)
        CreateReservationRequest invalidRequest = new CreateReservationRequest();
        invalidRequest.quantity = -1; // Invalid quantity

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
        .when()
                .post("/api/v1/inventory/reservations")
        .then()
                .statusCode(400);
    }
}
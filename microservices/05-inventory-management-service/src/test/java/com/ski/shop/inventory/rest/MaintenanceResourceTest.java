package com.ski.shop.inventory.rest;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.domain.MaintenanceRecord;
import com.ski.shop.inventory.dto.MaintenanceRecordDto;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class MaintenanceResourceTest {

    private Equipment testEquipment;

    @BeforeEach
    @TestTransaction
    void setUp() {
        MaintenanceRecord.deleteAll();
        Equipment.deleteAll();

        testEquipment = new Equipment();
        testEquipment.productId = UUID.randomUUID();
        testEquipment.cachedSku = "TEST-MAINTENANCE-001";
        testEquipment.cachedName = "Test Maintenance Equipment";
        testEquipment.cachedCategory = "Skis";
        testEquipment.dailyRate = new BigDecimal("60.00");
        testEquipment.availableQuantity = 6;
        testEquipment.reservedQuantity = 2;
        testEquipment.persist();
    }

    @Test
    void testCreateMaintenanceRecord() {
        MaintenanceRecordDto.CreateMaintenanceRequest request = new MaintenanceRecordDto.CreateMaintenanceRequest();
        request.equipmentId = testEquipment.id;
        request.maintenanceType = MaintenanceRecord.MaintenanceType.PREVENTIVE;
        request.title = "API Test Maintenance";
        request.description = "Test maintenance via API";
        request.scheduledDate = LocalDateTime.now().plusDays(5);
        request.technicianId = "API-TECH001";
        request.estimatedDurationMinutes = 90;
        request.priority = 2;

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/inventory/maintenance")
                .then()
                .statusCode(201)
                .body("equipmentId", equalTo(testEquipment.id.intValue()))
                .body("maintenanceType", equalTo("PREVENTIVE"))
                .body("title", equalTo("API Test Maintenance"))
                .body("status", equalTo("SCHEDULED"))
                .body("maintenanceId", notNullValue());
    }

    @Test
    void testGetMaintenanceRecord() {
        // Create maintenance record first
        MaintenanceRecord record = createTestMaintenanceRecord();

        given()
                .when()
                .get("/api/v1/inventory/maintenance/" + record.maintenanceId)
                .then()
                .statusCode(200)
                .body("maintenanceId", equalTo(record.maintenanceId.toString()))
                .body("title", equalTo("Test API Maintenance"))
                .body("status", equalTo("SCHEDULED"));
    }

    @Test
    void testGetMaintenanceRecordNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        given()
                .when()
                .get("/api/v1/inventory/maintenance/" + nonExistentId)
                .then()
                .statusCode(404)
                .body("error", equalTo("Maintenance record not found"));
    }

    @Test
    void testStartMaintenance() {
        MaintenanceRecord record = createTestMaintenanceRecord();

        MaintenanceRecordDto.StartMaintenanceRequest startRequest = new MaintenanceRecordDto.StartMaintenanceRequest();
        startRequest.technicianId = "START-TECH001";

        given()
                .contentType(ContentType.JSON)
                .body(startRequest)
                .when()
                .put("/api/v1/inventory/maintenance/" + record.maintenanceId + "/start")
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_PROGRESS"))
                .body("technicianId", equalTo("START-TECH001"))
                .body("startedDate", notNullValue());
    }

    @Test
    void testCompleteMaintenance() {
        // Create and start maintenance
        MaintenanceRecord record = createTestMaintenanceRecord();
        record.status = MaintenanceRecord.MaintenanceStatus.IN_PROGRESS;
        record.startedDate = LocalDateTime.now().minusHours(2);
        record.persist();

        MaintenanceRecordDto.CompleteMaintenanceRequest completeRequest = new MaintenanceRecordDto.CompleteMaintenanceRequest();
        completeRequest.notes = "Maintenance completed successfully via API";
        completeRequest.cost = new BigDecimal("120.75");
        completeRequest.partsUsed = "API test parts";

        given()
                .contentType(ContentType.JSON)
                .body(completeRequest)
                .when()
                .put("/api/v1/inventory/maintenance/" + record.maintenanceId + "/complete")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("notes", equalTo("Maintenance completed successfully via API"))
                .body("cost", equalTo(120.75f))
                .body("completedDate", notNullValue())
                .body("actualDurationMinutes", notNullValue());
    }

    @Test
    void testGetMaintenanceRecordsForEquipment() {
        // Create multiple maintenance records
        createTestMaintenanceRecord();
        createTestMaintenanceRecord();

        given()
                .when()
                .get("/api/v1/inventory/maintenance/equipment/" + testEquipment.id)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    void testGetOverdueMaintenanceRecords() {
        // Create overdue maintenance record
        MaintenanceRecord overdueRecord = new MaintenanceRecord();
        overdueRecord.equipmentId = testEquipment.id;
        overdueRecord.maintenanceType = MaintenanceRecord.MaintenanceType.SAFETY_INSPECTION;
        overdueRecord.title = "Overdue API Test";
        overdueRecord.scheduledDate = LocalDateTime.now().minusDays(3); // Overdue
        overdueRecord.priority = 1;
        overdueRecord.persist();

        given()
                .when()
                .get("/api/v1/inventory/maintenance/overdue")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].title", equalTo("Overdue API Test"))
                .body("[0].isOverdue", equalTo(true));
    }

    @Test
    void testGetUpcomingMaintenanceRecords() {
        // Create upcoming maintenance record
        MaintenanceRecord upcomingRecord = new MaintenanceRecord();
        upcomingRecord.equipmentId = testEquipment.id;
        upcomingRecord.maintenanceType = MaintenanceRecord.MaintenanceType.CLEANING;
        upcomingRecord.title = "Upcoming API Test";
        upcomingRecord.scheduledDate = LocalDateTime.now().plusDays(3); // Within 7 days
        upcomingRecord.priority = 3;
        upcomingRecord.persist();

        given()
                .queryParam("days", 7)
                .when()
                .get("/api/v1/inventory/maintenance/upcoming")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void testGenerateMaintenanceSchedule() {
        given()
                .queryParam("monthsAhead", 6)
                .when()
                .post("/api/v1/inventory/maintenance/equipment/" + testEquipment.id + "/schedule")
                .then()
                .statusCode(200)
                .body("message", equalTo("Maintenance schedule generated successfully"));
    }

    @Test
    void testGetMaintenanceStatistics() {
        // Create various maintenance records for statistics
        createTestMaintenanceRecord(); // SCHEDULED
        
        MaintenanceRecord inProgress = createTestMaintenanceRecord();
        inProgress.status = MaintenanceRecord.MaintenanceStatus.IN_PROGRESS;
        inProgress.persist();

        MaintenanceRecord completed = createTestMaintenanceRecord();
        completed.status = MaintenanceRecord.MaintenanceStatus.COMPLETED;
        completed.persist();

        given()
                .when()
                .get("/api/v1/inventory/maintenance/statistics")
                .then()
                .statusCode(200)
                .body("totalRecords", greaterThanOrEqualTo(3))
                .body("scheduledRecords", greaterThanOrEqualTo(1))
                .body("inProgressRecords", greaterThanOrEqualTo(1))
                .body("completedRecords", greaterThanOrEqualTo(1));
    }

    @Test
    void testCreateMaintenanceRecordInvalidEquipment() {
        MaintenanceRecordDto.CreateMaintenanceRequest request = new MaintenanceRecordDto.CreateMaintenanceRequest();
        request.equipmentId = 99999L; // Non-existent equipment
        request.maintenanceType = MaintenanceRecord.MaintenanceType.PREVENTIVE;
        request.title = "Invalid Equipment Test";
        request.scheduledDate = LocalDateTime.now().plusDays(1);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/inventory/maintenance")
                .then()
                .statusCode(400)
                .body("error", containsString("Equipment not found"));
    }

    private MaintenanceRecord createTestMaintenanceRecord() {
        MaintenanceRecord record = new MaintenanceRecord();
        record.equipmentId = testEquipment.id;
        record.maintenanceType = MaintenanceRecord.MaintenanceType.PREVENTIVE;
        record.title = "Test API Maintenance";
        record.description = "Test maintenance for API testing";
        record.scheduledDate = LocalDateTime.now().plusDays(7);
        record.technicianId = "TEST-TECH";
        record.estimatedDurationMinutes = 60;
        record.priority = 2;
        record.persist();
        return record;
    }
}
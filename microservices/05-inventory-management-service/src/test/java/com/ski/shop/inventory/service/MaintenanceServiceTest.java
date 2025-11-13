package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.domain.MaintenanceRecord;
import com.ski.shop.inventory.dto.MaintenanceRecordDto;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MaintenanceServiceTest {

    @Inject
    MaintenanceService maintenanceService;

    private Equipment testEquipment;

    @BeforeEach
    @TestTransaction
    void setUp() {
        // Clean up any existing data
        MaintenanceRecord.deleteAll();
        Equipment.deleteAll();

        // Create test equipment
        testEquipment = new Equipment();
        testEquipment.productId = UUID.randomUUID();
        testEquipment.cachedSku = "TEST-SKI-001";
        testEquipment.cachedName = "Test Ski Equipment";
        testEquipment.cachedCategory = "Skis";
        testEquipment.cachedBrand = "TestBrand";
        testEquipment.cachedEquipmentType = "Alpine";
        testEquipment.warehouseId = "WAREHOUSE-001";
        testEquipment.dailyRate = new BigDecimal("50.00");
        testEquipment.availableQuantity = 8;
        testEquipment.reservedQuantity = 2;
        testEquipment.persist();
    }

    @Test
    void testCreateMaintenanceRecord() {
        // Given - Setup equipment in same transaction as test
        Equipment equipment = new Equipment();
        equipment.productId = UUID.randomUUID();
        equipment.cachedSku = "TEST-SKI-001";
        equipment.cachedName = "Test Ski Equipment";
        equipment.cachedCategory = "Skis";
        equipment.cachedBrand = "TestBrand";
        equipment.cachedEquipmentType = "Alpine";
        equipment.warehouseId = "WAREHOUSE-001";
        equipment.dailyRate = new BigDecimal("50.00");
        equipment.availableQuantity = 8;
        equipment.reservedQuantity = 2;
        equipment.persist();

        MaintenanceRecordDto.CreateMaintenanceRequest request = new MaintenanceRecordDto.CreateMaintenanceRequest();
        request.equipmentId = equipment.id;
        request.maintenanceType = MaintenanceRecord.MaintenanceType.PREVENTIVE;
        request.title = "Test Maintenance";
        request.description = "Test maintenance description";
        request.scheduledDate = LocalDateTime.now().plusDays(7);
        request.technicianId = "TECH001";
        request.estimatedDurationMinutes = 120;
        request.priority = 2;

        // When
        MaintenanceRecordDto result = maintenanceService.createMaintenanceRecord(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.maintenanceId);
        assertEquals(equipment.id, result.equipmentId);
        assertEquals(MaintenanceRecord.MaintenanceType.PREVENTIVE, result.maintenanceType);
        assertEquals("Test Maintenance", result.title);
        assertEquals(MaintenanceRecord.MaintenanceStatus.SCHEDULED, result.status);
    }

    @Test
    @TestTransaction
    void testStartAndCompleteMaintenance() {
        // Given - Create maintenance record first
        MaintenanceRecordDto.CreateMaintenanceRequest createRequest = new MaintenanceRecordDto.CreateMaintenanceRequest();
        createRequest.equipmentId = testEquipment.id;
        createRequest.maintenanceType = MaintenanceRecord.MaintenanceType.CORRECTIVE;
        createRequest.title = "Test Repair";
        createRequest.scheduledDate = LocalDateTime.now().plusHours(1);

        MaintenanceRecordDto created = maintenanceService.createMaintenanceRecord(createRequest);

        // When - Start maintenance
        MaintenanceRecordDto.StartMaintenanceRequest startRequest = new MaintenanceRecordDto.StartMaintenanceRequest();
        startRequest.technicianId = "TECH002";

        Optional<MaintenanceRecordDto> started = maintenanceService.startMaintenance(created.maintenanceId, startRequest);

        // Then
        assertTrue(started.isPresent());
        assertEquals(MaintenanceRecord.MaintenanceStatus.IN_PROGRESS, started.get().status);
        assertEquals("TECH002", started.get().technicianId);
        assertNotNull(started.get().startedDate);

        // When - Complete maintenance
        MaintenanceRecordDto.CompleteMaintenanceRequest completeRequest = new MaintenanceRecordDto.CompleteMaintenanceRequest();
        completeRequest.notes = "Repair completed successfully";
        completeRequest.cost = new BigDecimal("75.50");
        completeRequest.partsUsed = "New brake pads";

        Optional<MaintenanceRecordDto> completed = maintenanceService.completeMaintenance(created.maintenanceId, completeRequest);

        // Then
        assertTrue(completed.isPresent());
        assertEquals(MaintenanceRecord.MaintenanceStatus.COMPLETED, completed.get().status);
        assertEquals("Repair completed successfully", completed.get().notes);
        assertEquals(new BigDecimal("75.50"), completed.get().cost);
        assertNotNull(completed.get().completedDate);
        assertNotNull(completed.get().actualDurationMinutes);
    }

    @Test
    @TestTransaction
    void testGetOverdueMaintenanceRecords() {
        // Given - Create overdue maintenance record
        MaintenanceRecordDto.CreateMaintenanceRequest request = new MaintenanceRecordDto.CreateMaintenanceRequest();
        request.equipmentId = testEquipment.id;
        request.maintenanceType = MaintenanceRecord.MaintenanceType.SAFETY_INSPECTION;
        request.title = "Overdue Safety Check";
        request.scheduledDate = LocalDateTime.now().minusDays(2); // Overdue

        maintenanceService.createMaintenanceRecord(request);

        // When
        List<MaintenanceRecordDto> overdueRecords = maintenanceService.getOverdueMaintenanceRecords();

        // Then
        assertFalse(overdueRecords.isEmpty());
        MaintenanceRecordDto overdue = overdueRecords.get(0);
        assertEquals("Overdue Safety Check", overdue.title);
        assertTrue(overdue.isOverdue);
    }

    @Test
    @TestTransaction
    void testGenerateMaintenanceSchedule() {
        // When - Use the test equipment ID
        maintenanceService.generateMaintenanceSchedule(testEquipment.id, 3);

        // Then
        List<MaintenanceRecordDto> futureRecords = maintenanceService.getMaintenanceRecordsForEquipment(testEquipment.id, 0, 10);
        assertFalse(futureRecords.isEmpty());
        
        // Check that maintenance is scheduled in the future
        boolean hasFutureMaintenance = futureRecords.stream()
                .anyMatch(record -> record.scheduledDate.isAfter(LocalDateTime.now()));
        assertTrue(hasFutureMaintenance);
    }

    @Test
    @TestTransaction
    void testGetMaintenanceStatistics() {
        // Given - Create various maintenance records
        createMaintenanceRecord(MaintenanceRecord.MaintenanceStatus.SCHEDULED, LocalDateTime.now().plusDays(1));
        createMaintenanceRecord(MaintenanceRecord.MaintenanceStatus.IN_PROGRESS, LocalDateTime.now());
        createMaintenanceRecord(MaintenanceRecord.MaintenanceStatus.COMPLETED, LocalDateTime.now().minusDays(1));
        createMaintenanceRecord(MaintenanceRecord.MaintenanceStatus.SCHEDULED, LocalDateTime.now().minusDays(1)); // Overdue

        // When
        MaintenanceService.MaintenanceStatistics stats = maintenanceService.getMaintenanceStatistics();

        // Then
        assertEquals(4, stats.totalRecords);
        assertEquals(2, stats.scheduledRecords);
        assertEquals(1, stats.inProgressRecords);
        assertEquals(1, stats.completedRecords);
        assertEquals(1, stats.overdueRecords);
    }

    private void createMaintenanceRecord(MaintenanceRecord.MaintenanceStatus status, LocalDateTime scheduledDate) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.equipmentId = testEquipment.id;
        record.maintenanceType = MaintenanceRecord.MaintenanceType.PREVENTIVE;
        record.status = status;
        record.title = "Test Maintenance - " + status;
        record.scheduledDate = scheduledDate;
        record.priority = 3;
        record.persist();
    }
}
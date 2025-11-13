package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.domain.InventoryAlert;
import com.ski.shop.inventory.dto.InventoryAlertDto;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AlertServiceTest {

    @Inject
    AlertService alertService;

    private Equipment testEquipment;

    @BeforeEach
    @TestTransaction
    void setUp() {
        // Clean up any existing data
        InventoryAlert.deleteAll();
        Equipment.deleteAll();

        // Create test equipment
        testEquipment = new Equipment();
        testEquipment.productId = UUID.randomUUID();
        testEquipment.cachedSku = "TEST-SKI-002";
        testEquipment.cachedName = "Test Alert Equipment";
        testEquipment.cachedCategory = "Boots";
        testEquipment.cachedBrand = "AlertBrand";
        testEquipment.warehouseId = "WAREHOUSE-002";
        testEquipment.dailyRate = new BigDecimal("40.00");
        testEquipment.availableQuantity = 2; // Low stock
        testEquipment.reservedQuantity = 3;
        testEquipment.persist();
    }

    @Test
    @TestTransaction
    void testCreateAlert() {
        // Given
        InventoryAlertDto.CreateAlertRequest request = new InventoryAlertDto.CreateAlertRequest();
        request.equipmentId = testEquipment.id;
        request.alertType = InventoryAlert.AlertType.LOW_STOCK;
        request.severity = InventoryAlert.AlertSeverity.HIGH;
        request.title = "Test Low Stock Alert";
        request.message = "Test equipment is running low on stock";
        request.thresholdValue = 5;
        request.currentValue = 2;

        // When
        InventoryAlertDto result = alertService.createAlert(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.alertId);
        assertEquals(testEquipment.id, result.equipmentId);
        assertEquals(InventoryAlert.AlertType.LOW_STOCK, result.alertType);
        assertEquals(InventoryAlert.AlertSeverity.HIGH, result.severity);
        assertEquals(InventoryAlert.AlertStatus.ACTIVE, result.status);
        assertTrue(result.isActive);
        assertTrue(result.isHighPriority);
    }

    @Test
    @TestTransaction
    void testAcknowledgeAndResolveAlert() {
        // Given - Create alert first
        InventoryAlertDto.CreateAlertRequest createRequest = new InventoryAlertDto.CreateAlertRequest();
        createRequest.alertType = InventoryAlert.AlertType.MAINTENANCE_DUE;
        createRequest.severity = InventoryAlert.AlertSeverity.MEDIUM;
        createRequest.title = "Test Maintenance Alert";
        createRequest.message = "Maintenance due for equipment";

        InventoryAlertDto created = alertService.createAlert(createRequest);

        // When - Acknowledge alert
        InventoryAlertDto.AcknowledgeAlertRequest ackRequest = new InventoryAlertDto.AcknowledgeAlertRequest();
        ackRequest.userId = "USER001";

        Optional<InventoryAlertDto> acknowledged = alertService.acknowledgeAlert(created.alertId, ackRequest);

        // Then
        assertTrue(acknowledged.isPresent());
        assertEquals(InventoryAlert.AlertStatus.ACKNOWLEDGED, acknowledged.get().status);
        assertEquals("USER001", acknowledged.get().acknowledgedBy);
        assertNotNull(acknowledged.get().acknowledgedAt);

        // When - Resolve alert
        InventoryAlertDto.ResolveAlertRequest resolveRequest = new InventoryAlertDto.ResolveAlertRequest();
        resolveRequest.userId = "USER002";
        resolveRequest.resolutionNotes = "Issue resolved successfully";

        Optional<InventoryAlertDto> resolved = alertService.resolveAlert(created.alertId, resolveRequest);

        // Then
        assertTrue(resolved.isPresent());
        assertEquals(InventoryAlert.AlertStatus.RESOLVED, resolved.get().status);
        assertEquals("USER002", resolved.get().resolvedBy);
        assertEquals("Issue resolved successfully", resolved.get().resolutionNotes);
        assertNotNull(resolved.get().resolvedAt);
        assertTrue(resolved.get().isResolved);
    }

    @Test
    @TestTransaction
    void testCreateLowStockAlert() {
        // When
        alertService.createLowStockAlert(testEquipment.id, 2, 5);

        // Then
        List<InventoryAlertDto> lowStockAlerts = alertService.getAlertsByType(InventoryAlert.AlertType.LOW_STOCK, 0, 10);
        assertFalse(lowStockAlerts.isEmpty());
        
        InventoryAlertDto alert = lowStockAlerts.get(0);
        assertEquals(testEquipment.id, alert.equipmentId);
        assertEquals(InventoryAlert.AlertType.LOW_STOCK, alert.alertType);
        assertEquals(InventoryAlert.AlertSeverity.HIGH, alert.severity);
        assertEquals(Integer.valueOf(5), alert.thresholdValue);
        assertEquals(Integer.valueOf(2), alert.currentValue);
    }

    @Test
    @TestTransaction
    void testCreateOutOfStockAlert() {
        // When
        alertService.createOutOfStockAlert(testEquipment.id);

        // Then
        List<InventoryAlertDto> outOfStockAlerts = alertService.getAlertsByType(InventoryAlert.AlertType.OUT_OF_STOCK, 0, 10);
        assertFalse(outOfStockAlerts.isEmpty());
        
        InventoryAlertDto alert = outOfStockAlerts.get(0);
        assertEquals(testEquipment.id, alert.equipmentId);
        assertEquals(InventoryAlert.AlertType.OUT_OF_STOCK, alert.alertType);
        assertEquals(InventoryAlert.AlertSeverity.CRITICAL, alert.severity);
        assertTrue(alert.isHighPriority);
    }

    @Test
    @TestTransaction
    void testCheckStockLevelsAndCreateAlerts() {
        // Given - Create equipment with various stock levels
        createEquipmentWithStock("OUT-001", 0); // Out of stock
        createEquipmentWithStock("LOW-001", 3); // Low stock
        createEquipmentWithStock("OK-001", 10); // Normal stock

        // When
        alertService.checkStockLevelsAndCreateAlerts();

        // Then
        List<InventoryAlertDto> activeAlerts = alertService.getActiveAlerts(0, 20);
        
        // Should have alerts for out of stock and low stock
        long outOfStockAlerts = activeAlerts.stream()
                .filter(alert -> alert.alertType == InventoryAlert.AlertType.OUT_OF_STOCK)
                .count();
        
        long lowStockAlerts = activeAlerts.stream()
                .filter(alert -> alert.alertType == InventoryAlert.AlertType.LOW_STOCK)
                .count();
        
        assertTrue(outOfStockAlerts >= 1);
        assertTrue(lowStockAlerts >= 1);
    }

    @Test
    @TestTransaction
    void testGetCriticalAlerts() {
        // Given - Create critical alert
        InventoryAlertDto.CreateAlertRequest criticalRequest = new InventoryAlertDto.CreateAlertRequest();
        criticalRequest.alertType = InventoryAlert.AlertType.SYSTEM_ERROR;
        criticalRequest.severity = InventoryAlert.AlertSeverity.CRITICAL;
        criticalRequest.title = "Critical System Error";
        criticalRequest.message = "Critical system error occurred";

        alertService.createAlert(criticalRequest);

        // Create non-critical alert
        InventoryAlertDto.CreateAlertRequest normalRequest = new InventoryAlertDto.CreateAlertRequest();
        normalRequest.alertType = InventoryAlert.AlertType.LOW_STOCK;
        normalRequest.severity = InventoryAlert.AlertSeverity.MEDIUM;
        normalRequest.title = "Normal Alert";
        normalRequest.message = "Normal alert message";

        alertService.createAlert(normalRequest);

        // When
        List<InventoryAlertDto> criticalAlerts = alertService.getCriticalAlerts();

        // Then
        assertFalse(criticalAlerts.isEmpty());
        assertEquals(1, criticalAlerts.size());
        assertEquals(InventoryAlert.AlertSeverity.CRITICAL, criticalAlerts.get(0).severity);
    }

    @Test
    @TestTransaction
    void testGetAlertStatistics() {
        // Given - Create various alerts
        createAlert(InventoryAlert.AlertType.LOW_STOCK, InventoryAlert.AlertSeverity.HIGH, InventoryAlert.AlertStatus.ACTIVE);
        createAlert(InventoryAlert.AlertType.OUT_OF_STOCK, InventoryAlert.AlertSeverity.CRITICAL, InventoryAlert.AlertStatus.ACTIVE);
        createAlert(InventoryAlert.AlertType.MAINTENANCE_DUE, InventoryAlert.AlertSeverity.MEDIUM, InventoryAlert.AlertStatus.ACKNOWLEDGED);
        createAlert(InventoryAlert.AlertType.SYSTEM_ERROR, InventoryAlert.AlertSeverity.HIGH, InventoryAlert.AlertStatus.RESOLVED);

        // When
        InventoryAlertDto.AlertStatistics stats = alertService.getAlertStatistics();

        // Then
        assertEquals(4, stats.totalAlerts);
        assertEquals(2, stats.activeAlerts);
        assertEquals(1, stats.acknowledgedAlerts);
        assertEquals(1, stats.resolvedAlerts);
        assertEquals(1, stats.criticalAlerts);
        assertEquals(2, stats.highPriorityAlerts); // HIGH + CRITICAL
        assertEquals(1, stats.lowStockAlerts);
        assertEquals(1, stats.maintenanceAlerts);
        assertEquals(1, stats.systemAlerts);
    }

    private void createEquipmentWithStock(String sku, int availableQuantity) {
        Equipment equipment = new Equipment();
        equipment.productId = UUID.randomUUID();
        equipment.cachedSku = sku;
        equipment.cachedName = "Test Equipment " + sku;
        equipment.cachedCategory = "TestCategory";
        equipment.dailyRate = new BigDecimal("30.00");
        equipment.availableQuantity = availableQuantity;
        equipment.reservedQuantity = 10 - availableQuantity;
        equipment.persist();
    }

    private void createAlert(InventoryAlert.AlertType type, InventoryAlert.AlertSeverity severity, InventoryAlert.AlertStatus status) {
        InventoryAlert alert = new InventoryAlert();
        alert.alertType = type;
        alert.severity = severity;
        alert.status = status;
        alert.title = "Test Alert - " + type;
        alert.message = "Test alert message";
        alert.persist();
    }
}
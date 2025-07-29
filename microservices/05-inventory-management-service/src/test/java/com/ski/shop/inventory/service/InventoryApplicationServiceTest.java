package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.Equipment;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(NoKafkaTestProfile.class)
class InventoryApplicationServiceTest {

    @Inject
    InventoryApplicationService inventoryService;

    @Inject
    EquipmentRepository equipmentRepository;

    private UUID testProductId;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up
        equipmentRepository.deleteAll();
        
        // Create test data
        testProductId = UUID.randomUUID();
        Equipment equipment = new Equipment(testProductId, "warehouse-001", BigDecimal.valueOf(50.00));
        equipment.updateCachedProductInfo(
                "TEST-SKI-001",
                "Test Ski Equipment",
                "SKI_BOARD",
                "TestBrand",
                "ALPINE_SKI",
                BigDecimal.valueOf(500.00)
        );
        equipment.addStock(10);
        
        equipmentRepository.persist(equipment);
    }

    @Test
    void testGetEquipmentDetail() {
        var equipmentDto = inventoryService.getEquipmentDetail(testProductId);
        
        assertNotNull(equipmentDto);
        assertEquals(testProductId, equipmentDto.productId);
        assertEquals("TEST-SKI-001", equipmentDto.sku);
        assertEquals("Test Ski Equipment", equipmentDto.name);
        assertEquals(10, equipmentDto.availableQuantity);
        assertEquals(0, equipmentDto.reservedQuantity);
        assertEquals(10, equipmentDto.totalQuantity);
        assertTrue(equipmentDto.isActive);
    }

    @Test
    void testCheckAvailability() {
        var availability = inventoryService.checkAvailability(
                testProductId, 
                java.time.LocalDateTime.now(), 
                java.time.LocalDateTime.now().plusDays(1), 
                5
        );
        
        assertNotNull(availability);
        assertEquals(testProductId, availability.productId);
        assertEquals(10, availability.totalQuantity);
        assertEquals(10, availability.availableQuantity);
        assertEquals(0, availability.reservedQuantity);
        assertTrue(availability.isAvailable);
        assertNotNull(availability.warehouseInfo);
        assertEquals("warehouse-001", availability.warehouseInfo.warehouseId);
    }

    @Test
    void testCheckAvailabilityInsufficientStock() {
        var availability = inventoryService.checkAvailability(
                testProductId, 
                java.time.LocalDateTime.now(), 
                java.time.LocalDateTime.now().plusDays(1), 
                15 // More than available
        );
        
        assertNotNull(availability);
        assertEquals(testProductId, availability.productId);
        assertFalse(availability.isAvailable);
    }

    @Test
    void testGetEquipmentDetailNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        var equipmentDto = inventoryService.getEquipmentDetail(nonExistentId);
        
        assertNull(equipmentDto);
    }
}
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
class EquipmentRepositoryTest {

    @Inject
    EquipmentRepository equipmentRepository;

    private UUID testProductId;
    private Equipment testEquipment;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing test data
        equipmentRepository.deleteAll();
        
        // Create test data
        testProductId = UUID.randomUUID();
        testEquipment = new Equipment(testProductId, "warehouse-001", BigDecimal.valueOf(50.00));
        testEquipment.updateCachedProductInfo(
                "TEST-SKI-001",
                "Test Ski Equipment",
                "SKI_BOARD",
                "TestBrand",
                "ALPINE_SKI",
                BigDecimal.valueOf(500.00)
        );
        testEquipment.addStock(10);
        
        equipmentRepository.persist(testEquipment);
    }

    @Test
    void testFindByProductId() {
        var found = equipmentRepository.findByProductId(testProductId);
        assertTrue(found.isPresent());
        assertEquals(testProductId, found.get().productId);
        assertEquals("TEST-SKI-001", found.get().cachedSku);
    }

    @Test
    void testFindByCachedSku() {
        var found = equipmentRepository.findByCachedSku("TEST-SKI-001");
        assertTrue(found.isPresent());
        assertEquals(testProductId, found.get().productId);
    }

    @Test
    @Transactional
    void testUpdateStockQuantity() {
        // Get the current version  
        var equipment = equipmentRepository.findByProductId(testProductId).orElseThrow();
        Long originalVersion = equipment.version;
        int originalQuantity = equipment.availableQuantity;
        
        // Flush to ensure the entity is in the database
        equipmentRepository.getEntityManager().flush();
        equipmentRepository.getEntityManager().clear();
        
        // Update stock
        boolean updated = equipmentRepository.updateStockQuantity(
                equipment.id, 5, originalVersion);
        
        assertTrue(updated);
        
        // Verify the update
        var updatedEquipment = equipmentRepository.findByProductId(testProductId).orElseThrow();
        assertEquals(originalQuantity + 5, updatedEquipment.availableQuantity.intValue());
        assertEquals(originalVersion + 1, updatedEquipment.version.intValue());
    }

    @Test
    @Transactional
    void testReserveStockAtomic() {
        var equipment = equipmentRepository.findByProductId(testProductId).orElseThrow();
        Long originalVersion = equipment.version;
        int originalAvailable = equipment.availableQuantity;
        int originalReserved = equipment.reservedQuantity;
        
        // Flush to ensure the entity is in the database
        equipmentRepository.getEntityManager().flush();
        equipmentRepository.getEntityManager().clear();
        
        // Reserve stock
        boolean reserved = equipmentRepository.reserveStockAtomic(
                testProductId, 3, originalVersion);
        
        assertTrue(reserved);
        
        // Verify the reservation
        var updatedEquipment = equipmentRepository.findByProductId(testProductId).orElseThrow();
        assertEquals(originalAvailable - 3, updatedEquipment.availableQuantity.intValue());
        assertEquals(originalReserved + 3, updatedEquipment.reservedQuantity.intValue());
        assertEquals(originalVersion + 1, updatedEquipment.version.intValue());
    }

    @Test
    @Transactional
    void testReserveStockAtomicInsufficientStock() {
        var equipment = equipmentRepository.findByProductId(testProductId).orElseThrow();
        Long originalVersion = equipment.version;
        
        // Try to reserve more than available
        boolean reserved = equipmentRepository.reserveStockAtomic(
                testProductId, 20, originalVersion);
        
        assertFalse(reserved);
    }

    @Test
    @Transactional
    void testFindLowStockItems() {
        // Create equipment with low stock
        UUID lowStockProductId = UUID.randomUUID();
        Equipment lowStockEquipment = new Equipment(lowStockProductId, "warehouse-001", BigDecimal.valueOf(30.00));
        lowStockEquipment.addStock(2); // Low stock
        equipmentRepository.persist(lowStockEquipment);
        
        var lowStockItems = equipmentRepository.findLowStockItems(5);
        assertTrue(lowStockItems.size() >= 1);
        assertTrue(lowStockItems.stream().anyMatch(eq -> eq.productId.equals(lowStockProductId)));
    }
}
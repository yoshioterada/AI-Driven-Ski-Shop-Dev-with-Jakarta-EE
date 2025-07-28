package com.skiresort.inventory.service;

import com.skiresort.inventory.event.ProductCreatedEvent;
import com.skiresort.inventory.model.Equipment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EquipmentServiceの単体テスト
 */
public class EquipmentServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Equipment> typedQuery;

    @InjectMocks
    private EquipmentService equipmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateEquipmentFromProduct() {
        // テストデータ準備
        ProductCreatedEvent event = new ProductCreatedEvent();
        event.setProductId(UUID.randomUUID());
        event.setSku("TEST-SKI-001");
        event.setName("Test Ski");
        event.setCategoryName("Ski Board");
        event.setBrandName("Test Brand");
        event.setEquipmentType("SKI_BOARD");
        event.setSizeRange("150-190cm");
        event.setDifficultyLevel("BEGINNER");
        event.setBasePrice(new BigDecimal("50000"));
        event.setDescription("Test ski for beginners");
        event.setImageUrl("/images/test-ski.jpg");
        event.setRentalAvailable(true);
        event.setActive(true);

        // モック設定: 既存設備なし
        when(entityManager.createNamedQuery("Equipment.findByProductId", Equipment.class))
                .thenReturn(typedQuery);
        when(typedQuery.setParameter("productId", event.getProductId()))
                .thenReturn(typedQuery);
        when(typedQuery.getSingleResult())
                .thenThrow(new jakarta.persistence.NoResultException());

        // テスト実行
        equipmentService.createEquipmentFromProduct(event);

        // 検証
        verify(entityManager).persist(any(Equipment.class));
    }

    @Test
    void testCreateEquipmentFromProductWithExistingEquipment() {
        // テストデータ準備
        ProductCreatedEvent event = new ProductCreatedEvent();
        event.setProductId(UUID.randomUUID());
        event.setSku("TEST-SKI-001");
        event.setName("Test Ski");
        event.setBasePrice(new BigDecimal("50000"));
        event.setEquipmentType("SKI_BOARD");
        event.setRentalAvailable(true);
        event.setActive(true);

        Equipment existingEquipment = new Equipment();
        existingEquipment.setId(1L);
        existingEquipment.setProductId(event.getProductId());

        // モック設定: 既存設備あり
        when(entityManager.createNamedQuery("Equipment.findByProductId", Equipment.class))
                .thenReturn(typedQuery);
        when(typedQuery.setParameter("productId", event.getProductId()))
                .thenReturn(typedQuery);
        when(typedQuery.getSingleResult())
                .thenReturn(existingEquipment);

        // テスト実行
        equipmentService.createEquipmentFromProduct(event);

        // 検証: 新規作成ではなく更新
        verify(entityManager, never()).persist(any(Equipment.class));
        verify(entityManager).merge(any(Equipment.class));
    }

    @Test
    void testCalculateDailyRate() {
        BigDecimal basePrice = new BigDecimal("50000");

        // SKI_BOARD: 10% × 1.2 = 12%
        BigDecimal skiRate = Equipment.calculateDailyRate(basePrice, "SKI_BOARD");
        assertEquals(new BigDecimal("6000.0"), skiRate);

        // BOOT: 10% × 1.1 = 11%
        BigDecimal bootRate = Equipment.calculateDailyRate(basePrice, "BOOT");
        assertEquals(new BigDecimal("5500.0"), bootRate);

        // HELMET: 10% × 0.8 = 8%
        BigDecimal helmetRate = Equipment.calculateDailyRate(basePrice, "HELMET");
        assertEquals(new BigDecimal("4000.0"), helmetRate);

        // POLE: 10% × 0.6 = 6%
        BigDecimal poleRate = Equipment.calculateDailyRate(basePrice, "POLE");
        assertEquals(new BigDecimal("3000.0"), poleRate);

        // その他: 10%
        BigDecimal otherRate = Equipment.calculateDailyRate(basePrice, "OTHER");
        assertEquals(new BigDecimal("5000.0"), otherRate);
    }

    @Test
    void testIsRentalEligible() {
        assertTrue(Equipment.isRentalEligible("SKI_BOARD"));
        assertTrue(Equipment.isRentalEligible("BOOT"));
        assertTrue(Equipment.isRentalEligible("HELMET"));
        assertFalse(Equipment.isRentalEligible("WAX"));
        assertFalse(Equipment.isRentalEligible("TUNING"));
    }

    @Test
    void testDeactivateEquipment() {
        UUID productId = UUID.randomUUID();
        Equipment equipment = new Equipment();
        equipment.setProductId(productId);
        equipment.setActive(true);

        // モック設定
        when(entityManager.createNamedQuery("Equipment.findByProductId", Equipment.class))
                .thenReturn(typedQuery);
        when(typedQuery.setParameter("productId", productId))
                .thenReturn(typedQuery);
        when(typedQuery.getSingleResult())
                .thenReturn(equipment);

        // テスト実行
        equipmentService.deactivateEquipment(productId);

        // 検証
        assertFalse(equipment.isActive());
        verify(entityManager).merge(equipment);
    }

    @Test
    void testActivateEquipment() {
        UUID productId = UUID.randomUUID();
        Equipment equipment = new Equipment();
        equipment.setProductId(productId);
        equipment.setActive(false);

        // モック設定
        when(entityManager.createNamedQuery("Equipment.findByProductId", Equipment.class))
                .thenReturn(typedQuery);
        when(typedQuery.setParameter("productId", productId))
                .thenReturn(typedQuery);
        when(typedQuery.getSingleResult())
                .thenReturn(equipment);

        // テスト実行
        equipmentService.activateEquipment(productId);

        // 検証
        assertTrue(equipment.isActive());
        verify(entityManager).merge(equipment);
    }
}
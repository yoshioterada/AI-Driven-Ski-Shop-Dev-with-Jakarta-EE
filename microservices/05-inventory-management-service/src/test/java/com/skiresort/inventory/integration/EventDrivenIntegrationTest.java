package com.skiresort.inventory.integration;

import com.skiresort.inventory.event.ProductCreatedEvent;
import com.skiresort.inventory.model.Equipment;
import com.skiresort.inventory.service.EquipmentService;
import com.skiresort.inventory.service.ProductEventConsumer;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * イベント駆動フローの統合テスト
 */
@QuarkusTest
public class EventDrivenIntegrationTest {

    @Inject
    ProductEventConsumer eventConsumer;

    @Inject
    EquipmentService equipmentService;

    @Test
    @Transactional
    public void testProductCreatedEventProcessing() throws Exception {
        // テスト用の商品作成イベントを準備
        ProductCreatedEvent event = new ProductCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("PRODUCT_CREATED");
        event.setAggregateId(UUID.randomUUID().toString());
        event.setProductId(UUID.fromString(event.getAggregateId()));
        event.setSku("TEST-SKI-001");
        event.setName("Test Ski for Integration");
        event.setCategoryName("Ski Board");
        event.setBrandName("Test Brand");
        event.setEquipmentType("SKI_BOARD");
        event.setSizeRange("150-190cm");
        event.setDifficultyLevel("INTERMEDIATE");
        event.setBasePrice(new BigDecimal("75000"));
        event.setDescription("Test ski for integration testing");
        event.setImageUrl("/images/test-ski-integration.jpg");
        event.setRentalAvailable(true);
        event.setActive(true);

        // モックメッセージを作成
        Message<ProductCreatedEvent> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn(event);
        Mockito.when(message.ack()).thenReturn(CompletableFuture.completedFuture(null));

        // イベント処理を実行
        eventConsumer.handleProductEvent(message).toCompletableFuture().get();

        // 設備が作成されたことを確認
        Equipment createdEquipment = equipmentService.findByProductId(event.getProductId());
        assertNotNull(createdEquipment);

        // 設備データの検証
        assertEquals(event.getProductId(), createdEquipment.getProductId());
        assertEquals(event.getSku(), createdEquipment.getCachedSku());
        assertEquals(event.getName(), createdEquipment.getCachedName());
        assertEquals(event.getCategoryName(), createdEquipment.getCachedCategory());
        assertEquals(event.getBrandName(), createdEquipment.getCachedBrand());
        assertEquals(event.getEquipmentType(), createdEquipment.getCachedEquipmentType());
        assertEquals(event.getDifficultyLevel(), createdEquipment.getCachedDifficultyLevel());
        assertEquals(event.getBasePrice(), createdEquipment.getCachedBasePrice());
        assertEquals(event.getDescription(), createdEquipment.getCachedDescription());
        assertEquals(event.getImageUrl(), createdEquipment.getCachedImageUrl());
        assertTrue(createdEquipment.isRentalAvailable());
        assertTrue(createdEquipment.isActive());

        // レンタル料金の計算が正しいことを確認
        BigDecimal expectedDailyRate = Equipment.calculateDailyRate(event.getBasePrice(), event.getEquipmentType());
        assertEquals(expectedDailyRate, createdEquipment.getDailyRate());

        // 旧フィールドも設定されていることを確認（段階的移行）
        assertEquals(event.getSku(), createdEquipment.getSku());
        assertEquals(event.getName(), createdEquipment.getName());

        // キャッシュ更新時刻が設定されていることを確認
        assertNotNull(createdEquipment.getCacheUpdatedAt());
    }

    @Test
    @Transactional 
    public void testNonRentalItemSkipped() throws Exception {
        // WAX商品のイベントを準備（レンタル対象外）
        ProductCreatedEvent waxEvent = new ProductCreatedEvent();
        waxEvent.setEventId(UUID.randomUUID().toString());
        waxEvent.setEventType("PRODUCT_CREATED");
        waxEvent.setAggregateId(UUID.randomUUID().toString());
        waxEvent.setProductId(UUID.fromString(waxEvent.getAggregateId()));
        waxEvent.setSku("TEST-WAX-001");
        waxEvent.setName("Test Wax");
        waxEvent.setEquipmentType("WAX");
        waxEvent.setRentalAvailable(false); // WAXなのでレンタル対象外
        waxEvent.setActive(true);

        // モックメッセージを作成
        Message<ProductCreatedEvent> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn(waxEvent);
        Mockito.when(message.ack()).thenReturn(CompletableFuture.completedFuture(null));

        // イベント処理を実行
        eventConsumer.handleProductEvent(message).toCompletableFuture().get();

        // 設備が作成されていないことを確認（レンタル対象外のため）
        Equipment equipment = equipmentService.findByProductId(waxEvent.getProductId());
        assertNull(equipment, "WAX products should not create equipment entries");
    }

    @Test
    public void testDailyRateCalculation() {
        BigDecimal basePrice = new BigDecimal("60000");

        // 各機器タイプの料金計算をテスト
        assertEquals(new BigDecimal("7200.0"), Equipment.calculateDailyRate(basePrice, "SKI_BOARD"));
        assertEquals(new BigDecimal("6600.0"), Equipment.calculateDailyRate(basePrice, "BOOT"));
        assertEquals(new BigDecimal("4800.0"), Equipment.calculateDailyRate(basePrice, "HELMET"));
        assertEquals(new BigDecimal("3600.0"), Equipment.calculateDailyRate(basePrice, "POLE"));
        assertEquals(new BigDecimal("3000.0"), Equipment.calculateDailyRate(basePrice, "GOGGLE"));
        assertEquals(new BigDecimal("2400.0"), Equipment.calculateDailyRate(basePrice, "GLOVE"));
        assertEquals(new BigDecimal("6000.0"), Equipment.calculateDailyRate(basePrice, "OTHER"));
    }

    @Test
    public void testRentalEligibilityLogic() {
        // レンタル対象の機器
        assertTrue(Equipment.isRentalEligible("SKI_BOARD"));
        assertTrue(Equipment.isRentalEligible("BOOT"));
        assertTrue(Equipment.isRentalEligible("HELMET"));
        assertTrue(Equipment.isRentalEligible("POLE"));
        assertTrue(Equipment.isRentalEligible("GOGGLE"));
        assertTrue(Equipment.isRentalEligible("GLOVE"));

        // レンタル対象外の機器
        assertFalse(Equipment.isRentalEligible("WAX"));
        assertFalse(Equipment.isRentalEligible("TUNING"));
    }
}
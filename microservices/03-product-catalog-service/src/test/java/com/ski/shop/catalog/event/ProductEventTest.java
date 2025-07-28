package com.ski.shop.catalog.event;

import com.ski.shop.catalog.domain.*;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductEventの単体テスト
 */
@QuarkusTest
public class ProductEventTest {

    @Test
    public void testProductCreatedEvent() {
        // テスト用の商品データを作成
        Category category = new Category();
        category.id = UUID.randomUUID();
        category.name = "Ski Board";
        category.path = "/ski-board";

        Brand brand = new Brand();
        brand.id = UUID.randomUUID();
        brand.name = "Test Brand";

        Product product = new Product();
        product.id = UUID.randomUUID();
        product.sku = "TEST-SKI-001";
        product.name = "Test Ski";
        product.description = "Test ski for beginners";
        product.category = category;
        product.brand = brand;
        product.material = Material.COMPOSITE;
        product.skiType = SkiType.SKI_BOARD;
        product.difficultyLevel = DifficultyLevel.BEGINNER;
        product.basePrice = new BigDecimal("50000");
        product.isActive = true;

        // ProductCreatedEventを作成
        ProductCreatedEvent event = new ProductCreatedEvent(product);

        // アサーション
        assertNotNull(event.getEventId());
        assertEquals("PRODUCT_CREATED", event.getEventType());
        assertEquals(product.id.toString(), event.getAggregateId());
        assertEquals(product.id, event.getProductId());
        assertEquals(product.sku, event.getSku());
        assertEquals(product.name, event.getName());
        assertEquals(category.name, event.getCategoryName());
        assertEquals(brand.name, event.getBrandName());
        assertEquals("SKI_BOARD", event.getEquipmentType());
        assertEquals("150-190cm", event.getSizeRange()); // デフォルトサイズ範囲
        assertEquals("BEGINNER", event.getDifficultyLevel());
        assertEquals(product.basePrice, event.getBasePrice());
        assertEquals(product.description, event.getDescription());
        assertTrue(event.isRentalAvailable()); // WAXやTUNINGではないのでtrue
        assertTrue(event.isActive());
    }

    @Test
    public void testProductCreatedEventForNonRentalItem() {
        // WAX商品のテスト（レンタル対象外）
        Category category = new Category();
        category.id = UUID.randomUUID();
        category.name = "Wax";
        category.path = "/wax";

        Brand brand = new Brand();
        brand.id = UUID.randomUUID();
        brand.name = "Test Brand";

        Product product = new Product();
        product.id = UUID.randomUUID();
        product.sku = "TEST-WAX-001";
        product.name = "Test Wax";
        product.category = category;
        product.brand = brand;
        product.material = Material.COMPOSITE;
        product.skiType = SkiType.WAX;
        product.difficultyLevel = DifficultyLevel.BEGINNER;
        product.basePrice = new BigDecimal("2000");
        product.isActive = true;

        ProductCreatedEvent event = new ProductCreatedEvent(product);

        assertEquals("WAX", event.getEquipmentType());
        assertFalse(event.isRentalAvailable()); // WAXなのでfalse
    }

    @Test
    public void testProductDeletedEvent() {
        UUID productId = UUID.randomUUID();
        String sku = "TEST-SKI-001";

        ProductDeletedEvent event = new ProductDeletedEvent(productId, sku);

        assertNotNull(event.getEventId());
        assertEquals("PRODUCT_DELETED", event.getEventType());
        assertEquals(productId.toString(), event.getAggregateId());
        assertEquals(productId, event.getProductId());
        assertEquals(sku, event.getSku());
        assertNotNull(event.getDeletedAt());
    }

    @Test
    public void testProductActivatedEvent() {
        UUID productId = UUID.randomUUID();
        String sku = "TEST-SKI-001";

        ProductActivatedEvent event = new ProductActivatedEvent(productId, sku);

        assertEquals("PRODUCT_ACTIVATED", event.getEventType());
        assertEquals(productId, event.getProductId());
        assertEquals(sku, event.getSku());
    }
}
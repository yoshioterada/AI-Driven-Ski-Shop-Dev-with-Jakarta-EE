package com.skiresort.inventory.service;

import com.skiresort.inventory.event.*;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

/**
 * 商品イベント購読サービス
 */
@ApplicationScoped
public class ProductEventConsumer {
    
    @Inject
    Logger logger;
    
    @Inject
    EquipmentService equipmentService;
    
    @Incoming("product-events")
    public CompletionStage<Void> handleProductEvent(Message<ProductEvent> message) {
        ProductEvent event = message.getPayload();
        
        logger.info("Received product event: " + event.getEventType() + " for product: " + event.getAggregateId());
        
        try {
            switch (event.getEventType()) {
                case "PRODUCT_CREATED":
                    handleProductCreated((ProductCreatedEvent) event);
                    break;
                case "PRODUCT_UPDATED":
                    handleProductUpdated((ProductUpdatedEvent) event);
                    break;
                case "PRODUCT_DELETED":
                    handleProductDeleted((ProductDeletedEvent) event);
                    break;
                case "PRODUCT_ACTIVATED":
                    handleProductActivated((ProductActivatedEvent) event);
                    break;
                case "PRODUCT_DEACTIVATED":
                    handleProductDeactivated((ProductDeactivatedEvent) event);
                    break;
                default:
                    logger.warning("Unknown product event type: " + event.getEventType());
            }
            
            logger.info("Successfully processed product event: " + event.getEventId());
            return message.ack();
            
        } catch (Exception e) {
            logger.severe("Failed to process product event: " + event.getEventId() + ", error: " + e.getMessage());
            // リトライメカニズムのためnackで処理失敗を通知
            return message.nack(e);
        }
    }
    
    private void handleProductCreated(ProductCreatedEvent event) {
        if (isRentalEligible(event)) {
            equipmentService.createEquipmentFromProduct(event);
            logger.info("Created equipment for product: " + event.getProductId());
        } else {
            logger.info("Product not eligible for rental, skipping equipment creation: " + event.getProductId());
        }
    }
    
    private void handleProductUpdated(ProductUpdatedEvent event) {
        equipmentService.updateEquipmentFromProduct(event);
        logger.info("Updated equipment cache for product: " + event.getProductId());
    }
    
    private void handleProductDeleted(ProductDeletedEvent event) {
        equipmentService.deactivateEquipment(event.getProductId());
        logger.info("Deactivated equipment for deleted product: " + event.getProductId());
    }
    
    private void handleProductActivated(ProductActivatedEvent event) {
        equipmentService.activateEquipment(event.getProductId());
        logger.info("Activated equipment for product: " + event.getProductId());
    }
    
    private void handleProductDeactivated(ProductDeactivatedEvent event) {
        equipmentService.deactivateEquipment(event.getProductId());
        logger.info("Deactivated equipment for product: " + event.getProductId());
    }
    
    /**
     * レンタル対象商品の判定ロジック
     */
    private boolean isRentalEligible(ProductCreatedEvent event) {
        return event.isRentalAvailable() && 
               !"WAX".equals(event.getEquipmentType()) && 
               !"TUNING".equals(event.getEquipmentType());
    }
}
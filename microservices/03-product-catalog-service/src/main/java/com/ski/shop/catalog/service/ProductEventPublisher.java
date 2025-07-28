package com.ski.shop.catalog.service;

import com.ski.shop.catalog.domain.Product;
import com.ski.shop.catalog.event.*;
import io.smallrye.reactive.messaging.annotations.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

/**
 * 商品イベント発行サービス
 */
@ApplicationScoped
public class ProductEventPublisher {
    
    @Channel("product-events-out")
    Emitter<ProductEvent> eventEmitter;
    
    @Inject
    Logger logger;
    
    public void publishProductCreated(Product product) {
        ProductCreatedEvent event = new ProductCreatedEvent(product);
        eventEmitter.send(event)
            .whenComplete((success, failure) -> {
                if (failure != null) {
                    logger.error("Failed to publish product created event for product: " + product.id, failure);
                } else {
                    logger.info("Published product created event for product: " + product.id);
                }
            });
    }
    
    public void publishProductUpdated(Product oldProduct, Product newProduct) {
        ProductUpdatedEvent event = new ProductUpdatedEvent(oldProduct, newProduct);
        eventEmitter.send(event)
            .whenComplete((success, failure) -> {
                if (failure != null) {
                    logger.error("Failed to publish product updated event for product: " + newProduct.id, failure);
                } else {
                    logger.info("Published product updated event for product: " + newProduct.id);
                }
            });
    }
    
    public void publishProductDeleted(UUID productId, String sku) {
        ProductDeletedEvent event = new ProductDeletedEvent(productId, sku);
        eventEmitter.send(event)
            .whenComplete((success, failure) -> {
                if (failure != null) {
                    logger.error("Failed to publish product deleted event for product: " + productId, failure);
                } else {
                    logger.info("Published product deleted event for product: " + productId);
                }
            });
    }
    
    public void publishProductActivated(UUID productId, String sku) {
        ProductActivatedEvent event = new ProductActivatedEvent(productId, sku);
        eventEmitter.send(event)
            .whenComplete((success, failure) -> {
                if (failure != null) {
                    logger.error("Failed to publish product activated event for product: " + productId, failure);
                } else {
                    logger.info("Published product activated event for product: " + productId);
                }
            });
    }
    
    public void publishProductDeactivated(UUID productId, String sku) {
        ProductDeactivatedEvent event = new ProductDeactivatedEvent(productId, sku);
        eventEmitter.send(event)
            .whenComplete((success, failure) -> {
                if (failure != null) {
                    logger.error("Failed to publish product deactivated event for product: " + productId, failure);
                } else {
                    logger.info("Published product deactivated event for product: " + productId);
                }
            });
    }
}
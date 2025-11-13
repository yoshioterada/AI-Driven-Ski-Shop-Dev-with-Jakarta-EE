package com.ski.shop.inventory.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ski.shop.inventory.service.ProductEventService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Kafka event consumer for Product Catalog events
 */
@ApplicationScoped
public class ProductEventConsumer {

    @Inject
    ProductEventService productEventService;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("product-events")
    public void handleProductEvent(String eventJson) {
        Log.debugf("Received product event: %s", eventJson);
        
        try {
            // Parse the event to determine type
            var eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.get("eventType").asText();
            
            switch (eventType) {
                case "ProductCreated":
                    handleProductCreatedEvent(eventJson);
                    break;
                case "ProductUpdated":
                    handleProductUpdatedEvent(eventJson);
                    break;
                case "ProductDeleted":
                    handleProductDeletedEvent(eventJson);
                    break;
                case "ProductPriceChanged":
                    handleProductPriceChangedEvent(eventJson);
                    break;
                default:
                    Log.warnf("Unknown event type: %s", eventType);
            }
        } catch (Exception e) {
            Log.errorf(e, "Error processing product event: %s", eventJson);
            // In production, this should be sent to a dead letter queue
        }
    }

    private void handleProductCreatedEvent(String eventJson) {
        try {
            ProductEvents.ProductCreatedEvent event = objectMapper.readValue(eventJson, ProductEvents.ProductCreatedEvent.class);
            Log.infof("Processing ProductCreated event for product ID: %s", event.productId);
            
            productEventService.handleProductCreated(event);
            
            Log.infof("Successfully processed ProductCreated event for product ID: %s", event.productId);
        } catch (Exception e) {
            Log.errorf(e, "Error handling ProductCreated event: %s", eventJson);
            throw new RuntimeException("Failed to process ProductCreated event", e);
        }
    }

    private void handleProductUpdatedEvent(String eventJson) {
        try {
            ProductEvents.ProductUpdatedEvent event = objectMapper.readValue(eventJson, ProductEvents.ProductUpdatedEvent.class);
            Log.infof("Processing ProductUpdated event for product ID: %s", event.productId);
            
            productEventService.handleProductUpdated(event);
            
            Log.infof("Successfully processed ProductUpdated event for product ID: %s", event.productId);
        } catch (Exception e) {
            Log.errorf(e, "Error handling ProductUpdated event: %s", eventJson);
            throw new RuntimeException("Failed to process ProductUpdated event", e);
        }
    }

    private void handleProductDeletedEvent(String eventJson) {
        try {
            ProductEvents.ProductDeletedEvent event = objectMapper.readValue(eventJson, ProductEvents.ProductDeletedEvent.class);
            Log.infof("Processing ProductDeleted event for product ID: %s", event.productId);
            
            productEventService.handleProductDeleted(event);
            
            Log.infof("Successfully processed ProductDeleted event for product ID: %s", event.productId);
        } catch (Exception e) {
            Log.errorf(e, "Error handling ProductDeleted event: %s", eventJson);
            throw new RuntimeException("Failed to process ProductDeleted event", e);
        }
    }

    private void handleProductPriceChangedEvent(String eventJson) {
        try {
            ProductEvents.ProductPriceChangedEvent event = objectMapper.readValue(eventJson, ProductEvents.ProductPriceChangedEvent.class);
            Log.infof("Processing ProductPriceChanged event for product ID: %s", event.productId);
            
            productEventService.handleProductPriceChanged(event);
            
            Log.infof("Successfully processed ProductPriceChanged event for product ID: %s", event.productId);
        } catch (Exception e) {
            Log.errorf(e, "Error handling ProductPriceChanged event: %s", eventJson);
            throw new RuntimeException("Failed to process ProductPriceChanged event", e);
        }
    }
}
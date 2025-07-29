package com.ski.shop.inventory.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Liveness health check for inventory service
 */
@Liveness
@ApplicationScoped
public class InventoryLivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        // Simple liveness check - service is alive if it can respond
        return HealthCheckResponse.named("inventory-liveness")
                .up()
                .withData("service", "inventory-management")
                .withData("status", "alive")
                .withData("timestamp", System.currentTimeMillis())
                .build();
    }
}
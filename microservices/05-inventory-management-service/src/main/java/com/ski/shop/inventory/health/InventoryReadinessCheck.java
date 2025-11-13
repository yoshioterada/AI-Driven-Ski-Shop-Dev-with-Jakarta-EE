package com.ski.shop.inventory.health;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.domain.InventoryAlert;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness health check for inventory service
 */
@Readiness
@ApplicationScoped
public class InventoryReadinessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        try {
            // Check database connectivity
            long equipmentCount = Equipment.count();
            
            // Check for critical alerts
            long criticalAlerts = InventoryAlert.count("severity = ?1 AND status = ?2", 
                    InventoryAlert.AlertSeverity.CRITICAL, InventoryAlert.AlertStatus.ACTIVE);
            
            return HealthCheckResponse.named("inventory-readiness")
                    .status(criticalAlerts < 10) // Service not ready if too many critical alerts
                    .withData("equipment_count", equipmentCount)
                    .withData("critical_alerts", criticalAlerts)
                    .withData("database_accessible", true)
                    .build();
                    
        } catch (Exception e) {
            return HealthCheckResponse.named("inventory-readiness")
                    .down()
                    .withData("database_accessible", false)
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
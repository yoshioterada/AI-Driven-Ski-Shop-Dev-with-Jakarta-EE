package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.MaintenanceRecord;
import com.ski.shop.inventory.service.AlertService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for monitoring and automated tasks
 */
@ApplicationScoped
public class MonitoringScheduler {

    @Inject
    AlertService alertService;

    @Inject
    MaintenanceService maintenanceService;

    /**
     * Check stock levels and create alerts every 30 minutes
     */
    @Scheduled(every = "30m")
    @Transactional
    public void checkStockLevels() {
        try {
            Log.info("Starting scheduled stock level check");
            alertService.checkStockLevelsAndCreateAlerts();
            Log.info("Completed stock level check");
        } catch (Exception e) {
            Log.error("Failed to check stock levels", e);
            alertService.createSyncFailureAlert("Stock level check failed", e.getMessage());
        }
    }

    /**
     * Check for overdue maintenance every hour
     */
    @Scheduled(every = "1h")
    @Transactional
    public void checkOverdueMaintenance() {
        try {
            Log.info("Starting overdue maintenance check");
            
            List<MaintenanceRecord> overdueRecords = MaintenanceRecord.find(
                "status = ?1 AND scheduledDate < ?2", 
                MaintenanceRecord.MaintenanceStatus.SCHEDULED, 
                LocalDateTime.now()
            ).list();
            
            for (MaintenanceRecord record : overdueRecords) {
                // Create overdue maintenance alert
                alertService.createMaintenanceOverdueAlert(
                    record.equipmentId, 
                    record.maintenanceId, 
                    record.scheduledDate
                );
            }
            
            Log.info("Completed overdue maintenance check. Found " + overdueRecords.size() + " overdue items");
        } catch (Exception e) {
            Log.error("Failed to check overdue maintenance", e);
            alertService.createSyncFailureAlert("Overdue maintenance check failed", e.getMessage());
        }
    }

    /**
     * Check for upcoming maintenance (within 7 days) every 6 hours
     */
    @Scheduled(every = "6h")
    @Transactional
    public void checkUpcomingMaintenance() {
        try {
            Log.info("Starting upcoming maintenance check");
            
            LocalDateTime nowPlus7Days = LocalDateTime.now().plusDays(7);
            List<MaintenanceRecord> upcomingRecords = MaintenanceRecord.find(
                "status = ?1 AND scheduledDate BETWEEN ?2 AND ?3", 
                MaintenanceRecord.MaintenanceStatus.SCHEDULED,
                LocalDateTime.now(),
                nowPlus7Days
            ).list();
            
            for (MaintenanceRecord record : upcomingRecords) {
                // Check if alert already exists
                long existingAlerts = com.ski.shop.inventory.domain.InventoryAlert.count(
                    "equipmentId = ?1 AND alertType = ?2 AND status = ?3",
                    record.equipmentId,
                    com.ski.shop.inventory.domain.InventoryAlert.AlertType.MAINTENANCE_DUE,
                    com.ski.shop.inventory.domain.InventoryAlert.AlertStatus.ACTIVE
                );
                
                if (existingAlerts == 0) {
                    alertService.createMaintenanceDueAlert(
                        record.equipmentId, 
                        record.maintenanceId, 
                        record.scheduledDate
                    );
                }
            }
            
            Log.info("Completed upcoming maintenance check. Found " + upcomingRecords.size() + " upcoming items");
        } catch (Exception e) {
            Log.error("Failed to check upcoming maintenance", e);
            alertService.createSyncFailureAlert("Upcoming maintenance check failed", e.getMessage());
        }
    }

    /**
     * Clean up old resolved alerts every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldAlerts() {
        try {
            Log.info("Starting alert cleanup");
            
            // Clean up alerts older than 30 days
            long deletedCount = alertService.cleanupOldAlerts(30);
            
            Log.info("Completed alert cleanup. Deleted " + deletedCount + " old alerts");
        } catch (Exception e) {
            Log.error("Failed to cleanup old alerts", e);
        }
    }

    /**
     * Generate maintenance schedules for equipment without future maintenance every week
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    @Transactional
    public void generateMaintenanceSchedules() {
        try {
            Log.info("Starting maintenance schedule generation");
            
            // Find equipment without scheduled future maintenance
            List<com.ski.shop.inventory.domain.Equipment> equipmentList = 
                com.ski.shop.inventory.domain.Equipment.listAll();
            
            for (com.ski.shop.inventory.domain.Equipment equipment : equipmentList) {
                // Check if equipment has future maintenance scheduled
                long futureMaintenanceCount = MaintenanceRecord.count(
                    "equipmentId = ?1 AND status = ?2 AND scheduledDate > ?3",
                    equipment.id,
                    MaintenanceRecord.MaintenanceStatus.SCHEDULED,
                    LocalDateTime.now()
                );
                
                if (futureMaintenanceCount == 0) {
                    // Generate maintenance schedule for next 12 months
                    maintenanceService.generateMaintenanceSchedule(equipment.id, 4); // 4 quarters
                }
            }
            
            Log.info("Completed maintenance schedule generation");
        } catch (Exception e) {
            Log.error("Failed to generate maintenance schedules", e);
            alertService.createSyncFailureAlert("Maintenance schedule generation failed", e.getMessage());
        }
    }

    /**
     * Health check and system monitoring every 15 minutes
     */
    @Scheduled(every = "15m")
    public void performSystemHealthCheck() {
        try {
            Log.debug("Performing system health check");
            
            // Check critical system metrics
            long criticalAlerts = com.ski.shop.inventory.domain.InventoryAlert.count(
                "severity = ?1 AND status = ?2", 
                com.ski.shop.inventory.domain.InventoryAlert.AlertSeverity.CRITICAL,
                com.ski.shop.inventory.domain.InventoryAlert.AlertStatus.ACTIVE
            );
            
            long outOfStockItems = com.ski.shop.inventory.domain.Equipment.count("availableQuantity = 0");
            
            long overdueMaintenanceCount = MaintenanceRecord.count(
                "status = ?1 AND scheduledDate < ?2", 
                MaintenanceRecord.MaintenanceStatus.SCHEDULED, 
                LocalDateTime.now()
            );
            
            // Create system alert if critical thresholds are exceeded
            if (criticalAlerts > 10) {
                alertService.createSyncFailureAlert(
                    "System has too many critical alerts", 
                    "Critical alert count: " + criticalAlerts
                );
            }
            
            if (outOfStockItems > 20) {
                alertService.createSyncFailureAlert(
                    "High number of out-of-stock items", 
                    "Out-of-stock count: " + outOfStockItems
                );
            }
            
            Log.debug("System health check completed. Critical alerts: " + criticalAlerts + 
                     ", Out of stock: " + outOfStockItems + 
                     ", Overdue maintenance: " + overdueMaintenanceCount);
        } catch (Exception e) {
            Log.error("System health check failed", e);
        }
    }
}
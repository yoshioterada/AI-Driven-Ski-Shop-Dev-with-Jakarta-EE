package com.ski.shop.inventory.metrics;

import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.domain.InventoryAlert;
import com.ski.shop.inventory.domain.MaintenanceRecord;
import com.ski.shop.inventory.domain.StockReservation;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Custom metrics for inventory service monitoring
 * This provides methods to get current metrics values for monitoring dashboards
 */
@ApplicationScoped
public class InventoryMetrics {

    public long getTotalEquipment() {
        try {
            return Equipment.count();
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getAvailableEquipment() {
        try {
            return Equipment.count("availableQuantity > 0");
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getOutOfStockEquipment() {
        try {
            return Equipment.count("availableQuantity = 0");
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getLowStockEquipment() {
        try {
            return Equipment.count("availableQuantity > 0 AND availableQuantity <= 5");
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getActiveAlerts() {
        try {
            return InventoryAlert.count("status", InventoryAlert.AlertStatus.ACTIVE);
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getCriticalAlerts() {
        try {
            return InventoryAlert.count("severity = ?1 AND status = ?2", 
                    InventoryAlert.AlertSeverity.CRITICAL, InventoryAlert.AlertStatus.ACTIVE);
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getPendingReservations() {
        try {
            return StockReservation.count("status", StockReservation.ReservationStatus.PENDING);
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getActiveReservations() {
        try {
            return StockReservation.count("status", StockReservation.ReservationStatus.CONFIRMED);
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getOverdueMaintenance() {
        try {
            return MaintenanceRecord.count("status = ?1 AND scheduledDate < ?2", 
                    MaintenanceRecord.MaintenanceStatus.SCHEDULED, java.time.LocalDateTime.now());
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getMaintenanceInProgress() {
        try {
            return MaintenanceRecord.count("status", MaintenanceRecord.MaintenanceStatus.IN_PROGRESS);
        } catch (Exception e) {
            return 0L;
        }
    }

    public double getTotalInventoryValue() {
        try {
            Object result = Equipment.find("SELECT SUM(e.dailyRate * (e.availableQuantity + e.reservedQuantity)) FROM Equipment e")
                    .firstResult();
            return result != null ? ((Number) result).doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double getAvailableInventoryValue() {
        try {
            Object result = Equipment.find("SELECT SUM(e.dailyRate * e.availableQuantity) FROM Equipment e")
                    .firstResult();
            return result != null ? ((Number) result).doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
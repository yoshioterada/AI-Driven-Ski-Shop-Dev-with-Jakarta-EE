package com.ski.shop.inventory.rest;

import com.ski.shop.inventory.dto.InventoryAlertDto;
import com.ski.shop.inventory.service.AlertService;
import com.ski.shop.inventory.service.MaintenanceService;
import com.ski.shop.inventory.service.SynchronizationService;
import com.ski.shop.inventory.metrics.InventoryMetrics;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API for system monitoring and dashboard
 */
@Path("/api/v1/inventory/system")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SystemMonitoringResource {

    @Inject
    InventoryMetrics inventoryMetrics;

    @Inject
    AlertService alertService;

    @Inject
    MaintenanceService maintenanceService;

    @Inject
    SynchronizationService synchronizationService;

    /**
     * Get comprehensive system status
     */
    @GET
    @Path("/status")
    public Response getSystemStatus() {
        try {
            SystemStatus status = new SystemStatus();
            
            // Basic inventory metrics
            status.totalEquipment = inventoryMetrics.getTotalEquipment();
            status.availableEquipment = inventoryMetrics.getAvailableEquipment();
            status.outOfStockEquipment = inventoryMetrics.getOutOfStockEquipment();
            status.lowStockEquipment = inventoryMetrics.getLowStockEquipment();
            
            // Alert metrics
            status.activeAlerts = inventoryMetrics.getActiveAlerts();
            status.criticalAlerts = inventoryMetrics.getCriticalAlerts();
            
            // Reservation metrics
            status.pendingReservations = inventoryMetrics.getPendingReservations();
            status.activeReservations = inventoryMetrics.getActiveReservations();
            
            // Maintenance metrics
            status.overdueMaintenance = inventoryMetrics.getOverdueMaintenance();
            status.maintenanceInProgress = inventoryMetrics.getMaintenanceInProgress();
            
            // Value metrics
            status.totalInventoryValue = inventoryMetrics.getTotalInventoryValue();
            status.availableInventoryValue = inventoryMetrics.getAvailableInventoryValue();
            
            // System health
            status.systemHealth = calculateSystemHealth(status);
            status.lastUpdated = LocalDateTime.now();
            
            return Response.ok(status).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get system status: " + e.getMessage())).build();
        }
    }

    /**
     * Get dashboard summary with key metrics and alerts
     */
    @GET
    @Path("/dashboard")
    public Response getDashboardSummary() {
        try {
            DashboardSummary dashboard = new DashboardSummary();
            
            // Key metrics
            dashboard.totalEquipment = inventoryMetrics.getTotalEquipment();
            dashboard.availableEquipment = inventoryMetrics.getAvailableEquipment();
            dashboard.activeAlerts = inventoryMetrics.getActiveAlerts();
            dashboard.criticalAlerts = inventoryMetrics.getCriticalAlerts();
            dashboard.pendingReservations = inventoryMetrics.getPendingReservations();
            
            // Critical alerts
            dashboard.criticalAlertsList = alertService.getCriticalAlerts();
            
            // Overdue maintenance
            dashboard.overdueMaintenance = maintenanceService.getOverdueMaintenanceRecords();
            
            // Alert statistics
            dashboard.alertStatistics = alertService.getAlertStatistics();
            
            // Maintenance statistics
            dashboard.maintenanceStatistics = maintenanceService.getMaintenanceStatistics();
            
            // Quick sync status
            List<SynchronizationService.InventoryDifference> differences = 
                    synchronizationService.detectDifferences();
            dashboard.syncIssues = differences.size();
            dashboard.hasSyncIssues = !differences.isEmpty();
            
            dashboard.lastUpdated = LocalDateTime.now();
            
            return Response.ok(dashboard).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get dashboard summary: " + e.getMessage())).build();
        }
    }

    /**
     * Get inventory KPIs (Key Performance Indicators)
     */
    @GET
    @Path("/kpis")
    public Response getInventoryKPIs() {
        try {
            InventoryKPIs kpis = new InventoryKPIs();
            
            long totalEquipment = inventoryMetrics.getTotalEquipment();
            long availableEquipment = inventoryMetrics.getAvailableEquipment();
            long outOfStock = inventoryMetrics.getOutOfStockEquipment();
            
            // Availability percentage
            kpis.availabilityPercentage = totalEquipment > 0 ? 
                    (double) availableEquipment / totalEquipment * 100 : 0;
            
            // Stock-out percentage
            kpis.stockOutPercentage = totalEquipment > 0 ? 
                    (double) outOfStock / totalEquipment * 100 : 0;
            
            // Alert response metrics
            long totalAlerts = inventoryMetrics.getActiveAlerts();
            long criticalAlerts = inventoryMetrics.getCriticalAlerts();
            kpis.criticalAlertRatio = totalAlerts > 0 ? 
                    (double) criticalAlerts / totalAlerts * 100 : 0;
            
            // Maintenance metrics
            long overdueMaintenance = inventoryMetrics.getOverdueMaintenance();
            long inProgressMaintenance = inventoryMetrics.getMaintenanceInProgress();
            kpis.maintenanceCompliancePercentage = (overdueMaintenance + inProgressMaintenance) > 0 ? 
                    (double) inProgressMaintenance / (overdueMaintenance + inProgressMaintenance) * 100 : 100;
            
            // Value utilization
            double totalValue = inventoryMetrics.getTotalInventoryValue();
            double availableValue = inventoryMetrics.getAvailableInventoryValue();
            kpis.valueUtilizationPercentage = totalValue > 0 ? 
                    (totalValue - availableValue) / totalValue * 100 : 0;
            
            kpis.lastCalculated = LocalDateTime.now();
            
            return Response.ok(kpis).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to calculate KPIs: " + e.getMessage())).build();
        }
    }

    /**
     * Get system health score and recommendations
     */
    @GET
    @Path("/health-score")
    public Response getSystemHealthScore() {
        try {
            SystemHealthScore healthScore = new SystemHealthScore();
            
            // Calculate health factors
            long totalEquipment = inventoryMetrics.getTotalEquipment();
            long availableEquipment = inventoryMetrics.getAvailableEquipment();
            long criticalAlerts = inventoryMetrics.getCriticalAlerts();
            long overdueMaintenance = inventoryMetrics.getOverdueMaintenance();
            long outOfStock = inventoryMetrics.getOutOfStockEquipment();
            
            // Availability score (0-25 points)
            double availabilityScore = totalEquipment > 0 ? 
                    (double) availableEquipment / totalEquipment * 25 : 0;
            
            // Alert score (0-25 points) - lower alerts = higher score
            double alertScore = criticalAlerts == 0 ? 25 : Math.max(0, 25 - criticalAlerts * 5);
            
            // Maintenance score (0-25 points) - no overdue = higher score
            double maintenanceScore = overdueMaintenance == 0 ? 25 : Math.max(0, 25 - overdueMaintenance * 3);
            
            // Stock score (0-25 points) - less out of stock = higher score
            double stockScore = totalEquipment > 0 ? 
                    (totalEquipment - outOfStock) / (double) totalEquipment * 25 : 0;
            
            healthScore.availabilityScore = availabilityScore;
            healthScore.alertScore = alertScore;
            healthScore.maintenanceScore = maintenanceScore;
            healthScore.stockScore = stockScore;
            healthScore.overallScore = availabilityScore + alertScore + maintenanceScore + stockScore;
            
            // Health status
            if (healthScore.overallScore >= 90) {
                healthScore.healthStatus = "EXCELLENT";
            } else if (healthScore.overallScore >= 75) {
                healthScore.healthStatus = "GOOD";
            } else if (healthScore.overallScore >= 60) {
                healthScore.healthStatus = "FAIR";
            } else if (healthScore.overallScore >= 40) {
                healthScore.healthStatus = "POOR";
            } else {
                healthScore.healthStatus = "CRITICAL";
            }
            
            // Recommendations
            if (criticalAlerts > 0) {
                healthScore.recommendations.add("Resolve " + criticalAlerts + " critical alerts immediately");
            }
            if (overdueMaintenance > 0) {
                healthScore.recommendations.add("Complete " + overdueMaintenance + " overdue maintenance tasks");
            }
            if (outOfStock > 0) {
                healthScore.recommendations.add("Restock " + outOfStock + " out-of-stock items");
            }
            
            healthScore.lastCalculated = LocalDateTime.now();
            
            return Response.ok(healthScore).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to calculate health score: " + e.getMessage())).build();
        }
    }

    // Helper method
    private String calculateSystemHealth(SystemStatus status) {
        if (status.criticalAlerts > 5 || status.overdueMaintenance > 10) {
            return "CRITICAL";
        } else if (status.activeAlerts > 20 || status.outOfStockEquipment > status.totalEquipment * 0.2) {
            return "WARNING";
        } else if (status.activeAlerts > 10 || status.lowStockEquipment > status.totalEquipment * 0.1) {
            return "CAUTION";
        } else {
            return "HEALTHY";
        }
    }

    // DTOs for responses
    public static class SystemStatus {
        public long totalEquipment;
        public long availableEquipment;
        public long outOfStockEquipment;
        public long lowStockEquipment;
        public long activeAlerts;
        public long criticalAlerts;
        public long pendingReservations;
        public long activeReservations;
        public long overdueMaintenance;
        public long maintenanceInProgress;
        public double totalInventoryValue;
        public double availableInventoryValue;
        public String systemHealth;
        public LocalDateTime lastUpdated;
    }

    public static class DashboardSummary {
        public long totalEquipment;
        public long availableEquipment;
        public long activeAlerts;
        public long criticalAlerts;
        public long pendingReservations;
        public List<com.ski.shop.inventory.dto.InventoryAlertDto> criticalAlertsList;
        public List<com.ski.shop.inventory.dto.MaintenanceRecordDto> overdueMaintenance;
        public InventoryAlertDto.AlertStatistics alertStatistics;
        public MaintenanceService.MaintenanceStatistics maintenanceStatistics;
        public int syncIssues;
        public boolean hasSyncIssues;
        public LocalDateTime lastUpdated;
    }

    public static class InventoryKPIs {
        public double availabilityPercentage;
        public double stockOutPercentage;
        public double criticalAlertRatio;
        public double maintenanceCompliancePercentage;
        public double valueUtilizationPercentage;
        public LocalDateTime lastCalculated;
    }

    public static class SystemHealthScore {
        public double availabilityScore;
        public double alertScore;
        public double maintenanceScore;
        public double stockScore;
        public double overallScore;
        public String healthStatus;
        public java.util.List<String> recommendations = new java.util.ArrayList<>();
        public LocalDateTime lastCalculated;
    }

    public static class ErrorResponse {
        public String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
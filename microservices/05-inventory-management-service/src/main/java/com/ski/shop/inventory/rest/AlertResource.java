package com.ski.shop.inventory.rest;

import com.ski.shop.inventory.domain.InventoryAlert;
import com.ski.shop.inventory.dto.InventoryAlertDto;
import com.ski.shop.inventory.service.AlertService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST API for alert management
 */
@Path("/api/v1/inventory/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlertResource {

    @Inject
    AlertService alertService;

    /**
     * Create a new alert
     */
    @POST
    public Response createAlert(InventoryAlertDto.CreateAlertRequest request) {
        try {
            InventoryAlertDto alert = alertService.createAlert(request);
            return Response.status(Response.Status.CREATED).entity(alert).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create alert")).build();
        }
    }

    /**
     * Get alert by ID
     */
    @GET
    @Path("/{alertId}")
    public Response getAlert(@PathParam("alertId") UUID alertId) {
        Optional<InventoryAlertDto> alert = alertService.getAlert(alertId);
        
        if (alert.isPresent()) {
            return Response.ok(alert.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Alert not found")).build();
        }
    }

    /**
     * Acknowledge an alert
     */
    @PUT
    @Path("/{alertId}/acknowledge")
    public Response acknowledgeAlert(@PathParam("alertId") UUID alertId,
                                   InventoryAlertDto.AcknowledgeAlertRequest request) {
        try {
            Optional<InventoryAlertDto> alert = alertService.acknowledgeAlert(alertId, request);
            
            if (alert.isPresent()) {
                return Response.ok(alert.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Alert not found")).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to acknowledge alert")).build();
        }
    }

    /**
     * Resolve an alert
     */
    @PUT
    @Path("/{alertId}/resolve")
    public Response resolveAlert(@PathParam("alertId") UUID alertId,
                               InventoryAlertDto.ResolveAlertRequest request) {
        try {
            Optional<InventoryAlertDto> alert = alertService.resolveAlert(alertId, request);
            
            if (alert.isPresent()) {
                return Response.ok(alert.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Alert not found")).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to resolve alert")).build();
        }
    }

    /**
     * Dismiss an alert
     */
    @PUT
    @Path("/{alertId}/dismiss")
    public Response dismissAlert(@PathParam("alertId") UUID alertId,
                               InventoryAlertDto.DismissAlertRequest request) {
        try {
            Optional<InventoryAlertDto> alert = alertService.dismissAlert(alertId, request);
            
            if (alert.isPresent()) {
                return Response.ok(alert.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Alert not found")).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to dismiss alert")).build();
        }
    }

    /**
     * Get active alerts
     */
    @GET
    @Path("/active")
    public Response getActiveAlerts(@QueryParam("page") @DefaultValue("0") int page,
                                  @QueryParam("size") @DefaultValue("20") int size) {
        try {
            List<InventoryAlertDto> alerts = alertService.getActiveAlerts(page, size);
            return Response.ok(alerts).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve active alerts")).build();
        }
    }

    /**
     * Get alerts by type
     */
    @GET
    @Path("/type/{alertType}")
    public Response getAlertsByType(@PathParam("alertType") InventoryAlert.AlertType alertType,
                                  @QueryParam("page") @DefaultValue("0") int page,
                                  @QueryParam("size") @DefaultValue("20") int size) {
        try {
            List<InventoryAlertDto> alerts = alertService.getAlertsByType(alertType, page, size);
            return Response.ok(alerts).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alerts by type")).build();
        }
    }

    /**
     * Get alerts by severity
     */
    @GET
    @Path("/severity/{severity}")
    public Response getAlertsBySeverity(@PathParam("severity") InventoryAlert.AlertSeverity severity,
                                      @QueryParam("page") @DefaultValue("0") int page,
                                      @QueryParam("size") @DefaultValue("20") int size) {
        try {
            List<InventoryAlertDto> alerts = alertService.getAlertsBySeverity(severity, page, size);
            return Response.ok(alerts).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alerts by severity")).build();
        }
    }

    /**
     * Get alerts for equipment
     */
    @GET
    @Path("/equipment/{equipmentId}")
    public Response getAlertsForEquipment(@PathParam("equipmentId") Long equipmentId,
                                        @QueryParam("page") @DefaultValue("0") int page,
                                        @QueryParam("size") @DefaultValue("20") int size) {
        try {
            List<InventoryAlertDto> alerts = alertService.getAlertsForEquipment(equipmentId, page, size);
            return Response.ok(alerts).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alerts for equipment")).build();
        }
    }

    /**
     * Get critical alerts
     */
    @GET
    @Path("/critical")
    public Response getCriticalAlerts() {
        try {
            List<InventoryAlertDto> alerts = alertService.getCriticalAlerts();
            return Response.ok(alerts).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve critical alerts")).build();
        }
    }

    /**
     * Trigger stock level check and create alerts
     */
    @POST
    @Path("/check-stock-levels")
    public Response checkStockLevels() {
        try {
            alertService.checkStockLevelsAndCreateAlerts();
            return Response.ok(new SuccessResponse("Stock level check completed")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to check stock levels")).build();
        }
    }

    /**
     * Get alert statistics
     */
    @GET
    @Path("/statistics")
    public Response getAlertStatistics() {
        try {
            InventoryAlertDto.AlertStatistics stats = alertService.getAlertStatistics();
            return Response.ok(stats).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alert statistics")).build();
        }
    }

    /**
     * Clean up old resolved alerts
     */
    @DELETE
    @Path("/cleanup")
    public Response cleanupOldAlerts(@QueryParam("daysOld") @DefaultValue("30") int daysOld) {
        try {
            long deletedCount = alertService.cleanupOldAlerts(daysOld);
            return Response.ok(new CleanupResponse("Cleaned up " + deletedCount + " old alerts")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to cleanup old alerts")).build();
        }
    }

    // Helper classes for responses
    public static class ErrorResponse {
        public String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    public static class SuccessResponse {
        public String message;
        
        public SuccessResponse(String message) {
            this.message = message;
        }
    }

    public static class CleanupResponse {
        public String message;
        
        public CleanupResponse(String message) {
            this.message = message;
        }
    }
}
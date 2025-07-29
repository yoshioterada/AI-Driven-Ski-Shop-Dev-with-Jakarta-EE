package com.ski.shop.inventory.rest;

import com.ski.shop.inventory.dto.MaintenanceRecordDto;
import com.ski.shop.inventory.service.MaintenanceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST API for maintenance management
 */
@Path("/api/v1/inventory/maintenance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaintenanceResource {

    @Inject
    MaintenanceService maintenanceService;

    /**
     * Create a new maintenance record
     */
    @POST
    public Response createMaintenanceRecord(MaintenanceRecordDto.CreateMaintenanceRequest request) {
        try {
            MaintenanceRecordDto record = maintenanceService.createMaintenanceRecord(request);
            return Response.status(Response.Status.CREATED).entity(record).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create maintenance record")).build();
        }
    }

    /**
     * Get maintenance record by ID
     */
    @GET
    @Path("/{maintenanceId}")
    public Response getMaintenanceRecord(@PathParam("maintenanceId") UUID maintenanceId) {
        Optional<MaintenanceRecordDto> record = maintenanceService.getMaintenanceRecord(maintenanceId);
        
        if (record.isPresent()) {
            return Response.ok(record.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Maintenance record not found")).build();
        }
    }

    /**
     * Update maintenance record
     */
    @PUT
    @Path("/{maintenanceId}")
    public Response updateMaintenanceRecord(@PathParam("maintenanceId") UUID maintenanceId,
                                          MaintenanceRecordDto.UpdateMaintenanceRequest request) {
        try {
            Optional<MaintenanceRecordDto> record = maintenanceService.updateMaintenanceRecord(maintenanceId, request);
            
            if (record.isPresent()) {
                return Response.ok(record.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Maintenance record not found")).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to update maintenance record")).build();
        }
    }

    /**
     * Start maintenance work
     */
    @PUT
    @Path("/{maintenanceId}/start")
    public Response startMaintenance(@PathParam("maintenanceId") UUID maintenanceId,
                                   MaintenanceRecordDto.StartMaintenanceRequest request) {
        try {
            Optional<MaintenanceRecordDto> record = maintenanceService.startMaintenance(maintenanceId, request);
            
            if (record.isPresent()) {
                return Response.ok(record.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Maintenance record not found")).build();
            }
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to start maintenance")).build();
        }
    }

    /**
     * Complete maintenance work
     */
    @PUT
    @Path("/{maintenanceId}/complete")
    public Response completeMaintenance(@PathParam("maintenanceId") UUID maintenanceId,
                                      MaintenanceRecordDto.CompleteMaintenanceRequest request) {
        try {
            Optional<MaintenanceRecordDto> record = maintenanceService.completeMaintenance(maintenanceId, request);
            
            if (record.isPresent()) {
                return Response.ok(record.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Maintenance record not found")).build();
            }
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to complete maintenance")).build();
        }
    }

    /**
     * Get maintenance records for equipment
     */
    @GET
    @Path("/equipment/{equipmentId}")
    public Response getMaintenanceRecordsForEquipment(@PathParam("equipmentId") Long equipmentId,
                                                    @QueryParam("page") @DefaultValue("0") int page,
                                                    @QueryParam("size") @DefaultValue("10") int size) {
        try {
            List<MaintenanceRecordDto> records = maintenanceService.getMaintenanceRecordsForEquipment(equipmentId, page, size);
            return Response.ok(records).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve maintenance records")).build();
        }
    }

    /**
     * Get overdue maintenance records
     */
    @GET
    @Path("/overdue")
    public Response getOverdueMaintenanceRecords() {
        try {
            List<MaintenanceRecordDto> records = maintenanceService.getOverdueMaintenanceRecords();
            return Response.ok(records).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve overdue maintenance records")).build();
        }
    }

    /**
     * Get upcoming maintenance records
     */
    @GET
    @Path("/upcoming")
    public Response getUpcomingMaintenanceRecords(@QueryParam("days") @DefaultValue("7") int days) {
        try {
            List<MaintenanceRecordDto> records = maintenanceService.getUpcomingMaintenanceRecords(days);
            return Response.ok(records).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve upcoming maintenance records")).build();
        }
    }

    /**
     * Generate maintenance schedule for equipment
     */
    @POST
    @Path("/equipment/{equipmentId}/schedule")
    public Response generateMaintenanceSchedule(@PathParam("equipmentId") Long equipmentId,
                                              @QueryParam("monthsAhead") @DefaultValue("12") int monthsAhead) {
        try {
            maintenanceService.generateMaintenanceSchedule(equipmentId, monthsAhead);
            return Response.ok(new SuccessResponse("Maintenance schedule generated successfully")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate maintenance schedule")).build();
        }
    }

    /**
     * Get maintenance statistics
     */
    @GET
    @Path("/statistics")
    public Response getMaintenanceStatistics() {
        try {
            MaintenanceService.MaintenanceStatistics stats = maintenanceService.getMaintenanceStatistics();
            return Response.ok(stats).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve maintenance statistics")).build();
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
}
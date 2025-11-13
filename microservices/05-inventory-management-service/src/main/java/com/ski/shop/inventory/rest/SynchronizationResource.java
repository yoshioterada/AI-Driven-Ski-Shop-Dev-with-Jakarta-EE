package com.ski.shop.inventory.rest;

import com.ski.shop.inventory.service.SynchronizationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST API for synchronization and consistency management
 */
@Path("/api/v1/inventory/sync")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SynchronizationResource {

    @Inject
    SynchronizationService synchronizationService;

    /**
     * Perform manual synchronization with Product Catalog Service
     */
    @POST
    @Path("/manual")
    public Response performManualSync() {
        try {
            SynchronizationService.SyncResult result = synchronizationService.performManualSync();
            
            if (result.success) {
                return Response.ok(result).build();
            } else {
                return Response.status(Response.Status.PARTIAL_CONTENT).entity(result).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Synchronization failed: " + e.getMessage())).build();
        }
    }

    /**
     * Detect differences between inventory and catalog
     */
    @GET
    @Path("/differences")
    public Response detectDifferences() {
        try {
            List<SynchronizationService.InventoryDifference> differences = 
                    synchronizationService.detectDifferences();
            return Response.ok(differences).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to detect differences: " + e.getMessage())).build();
        }
    }

    /**
     * Perform consistency check on inventory data
     */
    @GET
    @Path("/consistency-check")
    public Response performConsistencyCheck() {
        try {
            SynchronizationService.ConsistencyCheckResult result = 
                    synchronizationService.performConsistencyCheck();
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Consistency check failed: " + e.getMessage())).build();
        }
    }

    /**
     * Fix detected inconsistencies
     */
    @POST
    @Path("/fix-inconsistencies")
    public Response fixInconsistencies(FixInconsistenciesRequest request) {
        try {
            SynchronizationService.FixResult result = 
                    synchronizationService.fixInconsistencies(request.equipmentIds);
            
            if (result.errors.isEmpty()) {
                return Response.ok(result).build();
            } else {
                return Response.status(Response.Status.PARTIAL_CONTENT).entity(result).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fix inconsistencies: " + e.getMessage())).build();
        }
    }

    /**
     * Get synchronization status
     */
    @GET
    @Path("/status")
    public Response getSyncStatus() {
        try {
            SyncStatus status = new SyncStatus();
            
            // Get recent differences
            List<SynchronizationService.InventoryDifference> differences = 
                    synchronizationService.detectDifferences();
            status.totalDifferences = differences.size();
            status.hasInconsistencies = !differences.isEmpty();
            
            // Get consistency check summary
            SynchronizationService.ConsistencyCheckResult consistencyResult = 
                    synchronizationService.performConsistencyCheck();
            status.totalInconsistencies = consistencyResult.inconsistenciesFound;
            status.lastCheckTime = consistencyResult.checkStartTime;
            
            return Response.ok(status).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get sync status: " + e.getMessage())).build();
        }
    }

    // Request/Response DTOs
    public static class FixInconsistenciesRequest {
        public List<Long> equipmentIds;
    }

    public static class SyncStatus {
        public int totalDifferences;
        public boolean hasInconsistencies;
        public int totalInconsistencies;
        public java.time.LocalDateTime lastCheckTime;
    }

    public static class ErrorResponse {
        public String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
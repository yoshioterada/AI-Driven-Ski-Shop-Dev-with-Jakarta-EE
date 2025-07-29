package com.ski.shop.inventory.rest;

import com.ski.shop.inventory.dto.AvailabilityResponse;
import com.ski.shop.inventory.dto.EquipmentDto;
import com.ski.shop.inventory.dto.UpdateStockRequest;
import com.ski.shop.inventory.service.InventoryApplicationService;
import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST resource for inventory management operations
 */
@Path("/api/v1/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Inventory Management", description = "APIs for managing equipment inventory")
public class InventoryResource {

    @Inject
    InventoryApplicationService inventoryService;

    @GET
    @Path("/equipment")
    @Operation(summary = "Get equipment list", description = "Retrieve list of equipment with optional filtering")
    @APIResponse(responseCode = "200", description = "Equipment list retrieved successfully")
    public Response getEquipmentList(
            @Parameter(description = "Category filter") @QueryParam("category") String category,
            @Parameter(description = "Brand filter") @QueryParam("brand") String brand,
            @Parameter(description = "Warehouse ID filter") @QueryParam("warehouseId") String warehouseId,
            @Parameter(description = "Page number") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("20") int size) {
        
        try {
            Page pageRequest = Page.of(page, size);
            List<EquipmentDto> equipment = inventoryService.getEquipmentList(category, brand, warehouseId, pageRequest);
            return Response.ok(equipment).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving equipment list: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/equipment/{productId}")
    @Operation(summary = "Get equipment details", description = "Retrieve detailed information about specific equipment")
    @APIResponse(responseCode = "200", description = "Equipment details retrieved successfully")
    @APIResponse(responseCode = "404", description = "Equipment not found")
    public Response getEquipmentDetail(
            @Parameter(description = "Product ID") @PathParam("productId") UUID productId) {
        
        try {
            EquipmentDto equipment = inventoryService.getEquipmentDetail(productId);
            if (equipment != null) {
                return Response.ok(equipment).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Equipment not found for product ID: " + productId)
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving equipment details: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/equipment/{productId}/availability")
    @Operation(summary = "Check equipment availability", description = "Check stock availability for specific equipment")
    @APIResponse(responseCode = "200", description = "Availability information retrieved successfully")
    @APIResponse(responseCode = "404", description = "Equipment not found")
    public Response checkAvailability(
            @Parameter(description = "Product ID") @PathParam("productId") UUID productId,
            @Parameter(description = "Start date") @QueryParam("startDate") String startDate,
            @Parameter(description = "End date") @QueryParam("endDate") String endDate,
            @Parameter(description = "Required quantity") @QueryParam("quantity") @DefaultValue("1") Integer quantity) {
        
        try {
            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : LocalDateTime.now();
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : start.plusDays(1);
            
            AvailabilityResponse availability = inventoryService.checkAvailability(productId, start, end, quantity);
            if (availability != null) {
                return Response.ok(availability).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Equipment not found for product ID: " + productId)
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error checking availability: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/equipment/{productId}/stock")
    @Operation(summary = "Update stock level", description = "Add or remove stock for specific equipment")
    @APIResponse(responseCode = "200", description = "Stock updated successfully")
    @APIResponse(responseCode = "400", description = "Invalid request")
    @APIResponse(responseCode = "404", description = "Equipment not found")
    public Response updateStock(
            @Parameter(description = "Product ID") @PathParam("productId") UUID productId,
            @Valid UpdateStockRequest request) {
        
        try {
            boolean updated = inventoryService.updateStockLevel(productId, request);
            if (updated) {
                return Response.ok().entity("Stock updated successfully").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Equipment not found or update failed for product ID: " + productId)
                        .build();
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid request: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error updating stock: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/equipment/search")
    @Operation(summary = "Search equipment", description = "Search equipment by various criteria")
    @APIResponse(responseCode = "200", description = "Search results retrieved successfully")
    public Response searchEquipment(
            @Parameter(description = "SKU filter") @QueryParam("sku") String sku,
            @Parameter(description = "Name filter") @QueryParam("name") String name,
            @Parameter(description = "Category filter") @QueryParam("category") String category,
            @Parameter(description = "Equipment type filter") @QueryParam("type") String type,
            @Parameter(description = "Page number") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("20") int size) {
        
        try {
            Page pageRequest = Page.of(page, size);
            List<EquipmentDto> results = inventoryService.searchEquipment(sku, name, category, type, pageRequest);
            return Response.ok(results).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error searching equipment: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/health")
    @Operation(summary = "Health check", description = "Check service health status")
    @APIResponse(responseCode = "200", description = "Service is healthy")
    public Response healthCheck() {
        return Response.ok().entity("Inventory Management Service is running").build();
    }
}
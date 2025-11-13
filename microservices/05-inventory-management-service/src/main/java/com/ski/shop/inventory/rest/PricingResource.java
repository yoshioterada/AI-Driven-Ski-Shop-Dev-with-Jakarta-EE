package com.ski.shop.inventory.rest;

import com.ski.shop.inventory.dto.*;
import com.ski.shop.inventory.service.PricingCalculationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * REST API for pricing calculations
 */
@Path("/api/v1/inventory/pricing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Pricing", description = "Equipment rental pricing calculations")
public class PricingResource {

    private static final Logger LOG = Logger.getLogger(PricingResource.class);

    @Inject
    PricingCalculationService pricingService;

    @POST
    @Path("/calculate")
    @Operation(summary = "Calculate rental pricing", 
               description = "Calculate rental pricing for equipment with seasonal adjustments and discounts")
    @APIResponse(responseCode = "200", description = "Pricing calculated successfully",
                content = @Content(schema = @Schema(implementation = PricingCalculationResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request parameters")
    @APIResponse(responseCode = "404", description = "Equipment not found")
    public Response calculatePricing(@Valid PricingCalculationRequest request) {
        LOG.infof("Calculating pricing for product %s", request.productId);
        
        try {
            PricingCalculationResponse response = pricingService.calculatePricing(request);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            LOG.warnf("Invalid pricing request: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_REQUEST", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error calculating pricing for product %s", request.productId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("CALCULATION_ERROR", "Unable to calculate pricing"))
                    .build();
        }
    }

    @POST
    @Path("/bulk-calculate")
    @Operation(summary = "Calculate bulk rental pricing", 
               description = "Calculate pricing for multiple equipment rentals with bulk discounts")
    @APIResponse(responseCode = "200", description = "Bulk pricing calculated successfully",
                content = @Content(schema = @Schema(implementation = BulkPricingCalculationResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request parameters")
    public Response calculateBulkPricing(@Valid BulkPricingCalculationRequest request) {
        LOG.infof("Calculating bulk pricing for %d items", request.pricingRequests.size());
        
        try {
            BulkPricingCalculationResponse response = pricingService.calculateBulkPricing(request);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            LOG.warnf("Invalid bulk pricing request: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_REQUEST", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error calculating bulk pricing");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("CALCULATION_ERROR", "Unable to calculate bulk pricing"))
                    .build();
        }
    }

    @GET
    @Path("/rates/{productId}")
    @Operation(summary = "Get equipment pricing rates", 
               description = "Get current pricing rates and multipliers for specific equipment")
    @APIResponse(responseCode = "200", description = "Pricing rates retrieved successfully",
                content = @Content(schema = @Schema(implementation = EquipmentPricingRatesResponse.class)))
    @APIResponse(responseCode = "404", description = "Equipment not found")
    public Response getEquipmentPricingRates(@PathParam("productId") UUID productId) {
        LOG.infof("Getting pricing rates for product %s", productId);
        
        try {
            EquipmentPricingRatesResponse response = pricingService.getEquipmentPricingRates(productId);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            LOG.warnf("Equipment not found: %s", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("EQUIPMENT_NOT_FOUND", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error getting pricing rates for product %s", productId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("RATES_ERROR", "Unable to get pricing rates"))
                    .build();
        }
    }

    /**
     * Error response DTO
     */
    public static class ErrorResponse {
        public String error;
        public String message;

        public ErrorResponse() {}

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
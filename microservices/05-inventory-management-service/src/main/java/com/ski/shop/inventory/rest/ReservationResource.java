package com.ski.shop.inventory.rest;

import com.ski.shop.inventory.dto.CreateReservationRequest;
import com.ski.shop.inventory.dto.ExtendReservationRequest;
import com.ski.shop.inventory.dto.ReservationResponse;
import com.ski.shop.inventory.service.ReservationService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for stock reservation management
 */
@Path("/api/v1/inventory/reservations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Stock Reservations", description = "Stock reservation management operations")
public class ReservationResource {

    @Inject
    ReservationService reservationService;

    @POST
    @Operation(summary = "Create a new stock reservation", 
               description = "Creates a new stock reservation with automatic timeout management")
    @APIResponse(responseCode = "201", description = "Reservation created successfully",
                content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request data")
    @APIResponse(responseCode = "409", description = "Insufficient stock available")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response createReservation(@Valid CreateReservationRequest request) {
        try {
            Log.infof("Creating reservation for product %s, customer %s, quantity %d", 
                     request.productId, request.customerId, request.quantity);
            
            ReservationResponse reservation = reservationService.createReservation(request);
            return Response.status(Response.Status.CREATED).entity(reservation).build();
            
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid reservation request: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_REQUEST", e.getMessage()))
                    .build();
        } catch (IllegalStateException e) {
            Log.warnf("Reservation conflict: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("INSUFFICIENT_STOCK", e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Error creating reservation");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("INTERNAL_ERROR", "Failed to create reservation"))
                    .build();
        }
    }

    @GET
    @Path("/{reservationId}")
    @Operation(summary = "Get reservation details", 
               description = "Retrieves detailed information about a specific reservation")
    @APIResponse(responseCode = "200", description = "Reservation found",
                content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    @APIResponse(responseCode = "404", description = "Reservation not found")
    public Response getReservation(
            @Parameter(description = "Reservation ID", required = true)
            @PathParam("reservationId") UUID reservationId) {
        
        Log.debugf("Getting reservation details for %s", reservationId);
        
        ReservationResponse reservation = reservationService.getReservation(reservationId);
        if (reservation == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Reservation not found"))
                    .build();
        }
        
        return Response.ok(reservation).build();
    }

    @PUT
    @Path("/{reservationId}/confirm")
    @Operation(summary = "Confirm a pending reservation", 
               description = "Confirms a pending reservation and allocates the stock")
    @APIResponse(responseCode = "200", description = "Reservation confirmed successfully",
                content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    @APIResponse(responseCode = "404", description = "Reservation not found")
    @APIResponse(responseCode = "409", description = "Reservation cannot be confirmed")
    public Response confirmReservation(
            @Parameter(description = "Reservation ID", required = true)
            @PathParam("reservationId") UUID reservationId) {
        
        try {
            Log.infof("Confirming reservation %s", reservationId);
            
            ReservationResponse reservation = reservationService.confirmReservation(reservationId);
            return Response.ok(reservation).build();
            
        } catch (IllegalArgumentException e) {
            Log.warnf("Reservation not found: %s", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", e.getMessage()))
                    .build();
        } catch (IllegalStateException e) {
            Log.warnf("Cannot confirm reservation: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("CANNOT_CONFIRM", e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Error confirming reservation %s", reservationId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("INTERNAL_ERROR", "Failed to confirm reservation"))
                    .build();
        }
    }

    @PUT
    @Path("/{reservationId}/cancel")
    @Operation(summary = "Cancel a reservation", 
               description = "Cancels a reservation and releases any allocated stock")
    @APIResponse(responseCode = "200", description = "Reservation cancelled successfully",
                content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    @APIResponse(responseCode = "404", description = "Reservation not found")
    @APIResponse(responseCode = "409", description = "Reservation cannot be cancelled")
    public Response cancelReservation(
            @Parameter(description = "Reservation ID", required = true)
            @PathParam("reservationId") UUID reservationId,
            @Parameter(description = "Cancellation reason")
            @QueryParam("reason") String reason) {
        
        try {
            Log.infof("Cancelling reservation %s with reason: %s", reservationId, reason);
            
            ReservationResponse reservation = reservationService.cancelReservation(reservationId, reason);
            return Response.ok(reservation).build();
            
        } catch (IllegalArgumentException e) {
            Log.warnf("Reservation not found: %s", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", e.getMessage()))
                    .build();
        } catch (IllegalStateException e) {
            Log.warnf("Cannot cancel reservation: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("CANNOT_CANCEL", e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Error cancelling reservation %s", reservationId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("INTERNAL_ERROR", "Failed to cancel reservation"))
                    .build();
        }
    }

    @PUT
    @Path("/{reservationId}/extend")
    @Operation(summary = "Extend reservation timeout", 
               description = "Extends the expiration time of a pending reservation")
    @APIResponse(responseCode = "200", description = "Reservation extended successfully",
                content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    @APIResponse(responseCode = "404", description = "Reservation not found")
    @APIResponse(responseCode = "409", description = "Reservation cannot be extended")
    public Response extendReservation(
            @Parameter(description = "Reservation ID", required = true)
            @PathParam("reservationId") UUID reservationId,
            @Valid ExtendReservationRequest request) {
        
        try {
            Log.infof("Extending reservation %s by %d minutes", reservationId, request.additionalMinutes);
            
            ReservationResponse reservation = reservationService.extendReservation(reservationId, request);
            return Response.ok(reservation).build();
            
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid extend request: %s", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", e.getMessage()))
                    .build();
        } catch (IllegalStateException e) {
            Log.warnf("Cannot extend reservation: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("CANNOT_EXTEND", e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Error extending reservation %s", reservationId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("INTERNAL_ERROR", "Failed to extend reservation"))
                    .build();
        }
    }

    @GET
    @Path("/customer/{customerId}")
    @Operation(summary = "Get customer reservations", 
               description = "Retrieves all reservations for a specific customer")
    @APIResponse(responseCode = "200", description = "Customer reservations retrieved",
                content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    public Response getCustomerReservations(
            @Parameter(description = "Customer ID", required = true)
            @PathParam("customerId") String customerId) {
        
        Log.debugf("Getting reservations for customer %s", customerId);
        
        List<ReservationResponse> reservations = reservationService.getCustomerReservations(customerId);
        return Response.ok(reservations).build();
    }

    @GET
    @Path("/equipment/{productId}")
    @Operation(summary = "Get equipment reservations", 
               description = "Retrieves all reservations for specific equipment")
    @APIResponse(responseCode = "200", description = "Equipment reservations retrieved",
                content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    public Response getEquipmentReservations(
            @Parameter(description = "Product ID", required = true)
            @PathParam("productId") UUID productId) {
        
        Log.debugf("Getting reservations for product %s", productId);
        
        List<ReservationResponse> reservations = reservationService.getEquipmentReservations(productId);
        return Response.ok(reservations).build();
    }

    /**
     * Error response DTO
     */
    public static class ErrorResponse {
        public String errorCode;
        public String message;
        public long timestamp = System.currentTimeMillis();

        public ErrorResponse() {}

        public ErrorResponse(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }
    }
}
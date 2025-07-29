package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.StockReservation;
import com.ski.shop.inventory.event.ReservationEvents;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;

/**
 * Service for publishing reservation-related events to Kafka
 */
@ApplicationScoped
public class ReservationEventService {

    @Inject
    @Channel("reservation-events")
    Emitter<String> reservationEventsEmitter;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Publish reservation created event
     */
    public void publishReservationCreated(StockReservation reservation) {
        try {
            ReservationEvents.StockReservationCreatedEvent event = 
                new ReservationEvents.StockReservationCreatedEvent(reservation);
            
            String eventJson = objectMapper.writeValueAsString(event);
            reservationEventsEmitter.send(eventJson);
            
            Log.infof("Published StockReservationCreatedEvent for reservation %s", reservation.reservationId);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to publish StockReservationCreatedEvent for reservation %s", 
                      reservation.reservationId);
        }
    }

    /**
     * Publish reservation confirmed event
     */
    public void publishReservationConfirmed(StockReservation reservation) {
        try {
            ReservationEvents.StockReservationConfirmedEvent event = 
                new ReservationEvents.StockReservationConfirmedEvent(reservation);
            
            String eventJson = objectMapper.writeValueAsString(event);
            reservationEventsEmitter.send(eventJson);
            
            Log.infof("Published StockReservationConfirmedEvent for reservation %s", reservation.reservationId);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to publish StockReservationConfirmedEvent for reservation %s", 
                      reservation.reservationId);
        }
    }

    /**
     * Publish reservation cancelled event
     */
    public void publishReservationCancelled(StockReservation reservation, String reason) {
        try {
            ReservationEvents.StockReservationCancelledEvent event = 
                new ReservationEvents.StockReservationCancelledEvent(reservation, reason);
            
            String eventJson = objectMapper.writeValueAsString(event);
            reservationEventsEmitter.send(eventJson);
            
            Log.infof("Published StockReservationCancelledEvent for reservation %s", reservation.reservationId);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to publish StockReservationCancelledEvent for reservation %s", 
                      reservation.reservationId);
        }
    }

    /**
     * Publish reservation expired event
     */
    public void publishReservationExpired(StockReservation reservation) {
        try {
            ReservationEvents.StockReservationExpiredEvent event = 
                new ReservationEvents.StockReservationExpiredEvent(reservation);
            
            String eventJson = objectMapper.writeValueAsString(event);
            reservationEventsEmitter.send(eventJson);
            
            Log.infof("Published StockReservationExpiredEvent for reservation %s", reservation.reservationId);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to publish StockReservationExpiredEvent for reservation %s", 
                      reservation.reservationId);
        }
    }

    /**
     * Publish reservation expiring warning event
     */
    public void publishReservationExpiringWarning(StockReservation reservation) {
        try {
            ReservationEvents.StockReservationExpiringWarningEvent event = 
                new ReservationEvents.StockReservationExpiringWarningEvent(reservation);
            
            String eventJson = objectMapper.writeValueAsString(event);
            reservationEventsEmitter.send(eventJson);
            
            Log.debugf("Published StockReservationExpiringWarningEvent for reservation %s", 
                      reservation.reservationId);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to publish StockReservationExpiringWarningEvent for reservation %s", 
                      reservation.reservationId);
        }
    }
}
package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.StockReservation;
import com.ski.shop.inventory.dto.ReservationResponse;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Background scheduler for managing reservation timeouts and cleanup
 */
@ApplicationScoped
public class ReservationTimeoutScheduler {

    @Inject
    ReservationService reservationService;

    @Inject
    ReservationEventService reservationEventService;

    /**
     * Process expired reservations every minute
     */
    @Scheduled(every = "1m", identity = "process-expired-reservations")
    public void processExpiredReservations() {
        try {
            Log.debug("Starting expired reservations cleanup");
            int expiredCount = reservationService.processExpiredReservations();
            
            if (expiredCount > 0) {
                Log.infof("Expired %d reservations", expiredCount);
            }
        } catch (Exception e) {
            Log.errorf(e, "Error processing expired reservations");
        }
    }

    /**
     * Send warning notifications for reservations expiring soon (every 5 minutes)
     */
    @Scheduled(every = "5m", identity = "reservation-expiring-warnings")
    public void sendExpiringWarnings() {
        try {
            Log.debug("Checking for reservations expiring soon");
            
            // Get reservations expiring within 10 minutes
            List<ReservationResponse> expiringSoon = reservationService.getReservationsExpiringSoon(10);
            
            for (ReservationResponse reservationResponse : expiringSoon) {
                try {
                    // Find the actual reservation entity to publish event
                    StockReservation reservation = StockReservation.findByReservationId(reservationResponse.reservationId);
                    if (reservation != null && reservation.isPending()) {
                        reservationEventService.publishReservationExpiringWarning(reservation);
                    }
                } catch (Exception e) {
                    Log.errorf(e, "Failed to send expiring warning for reservation %s", 
                              reservationResponse.reservationId);
                }
            }
            
            if (!expiringSoon.isEmpty()) {
                Log.infof("Sent expiring warnings for %d reservations", expiringSoon.size());
            }
        } catch (Exception e) {
            Log.errorf(e, "Error sending expiring warnings");
        }
    }

    /**
     * Cleanup old cancelled and expired reservations (daily)
     */
    @Scheduled(cron = "0 0 2 * * ?", identity = "cleanup-old-reservations")
    public void cleanupOldReservations() {
        try {
            Log.info("Starting cleanup of old reservations");
            
            // This could be enhanced to archive old reservations to a separate table
            // For now, we'll just log the count of old reservations
            
            List<StockReservation> cancelledReservations = StockReservation.findByStatus(
                StockReservation.ReservationStatus.CANCELLED);
            List<StockReservation> expiredReservations = StockReservation.findByStatus(
                StockReservation.ReservationStatus.EXPIRED);
            
            Log.infof("Found %d cancelled and %d expired reservations for potential cleanup", 
                     cancelledReservations.size(), expiredReservations.size());
            
            // TODO: Implement archiving logic for reservations older than X days
            
        } catch (Exception e) {
            Log.errorf(e, "Error during old reservations cleanup");
        }
    }
}
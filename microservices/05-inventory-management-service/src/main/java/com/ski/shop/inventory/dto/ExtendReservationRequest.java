package com.ski.shop.inventory.dto;

import jakarta.validation.constraints.Positive;

/**
 * Request DTO for extending reservation timeout
 */
public class ExtendReservationRequest {

    @Positive
    public Integer additionalMinutes;

    public String reason;

    public ExtendReservationRequest() {}

    public ExtendReservationRequest(Integer additionalMinutes) {
        this.additionalMinutes = additionalMinutes;
    }

    public ExtendReservationRequest(Integer additionalMinutes, String reason) {
        this.additionalMinutes = additionalMinutes;
        this.reason = reason;
    }
}
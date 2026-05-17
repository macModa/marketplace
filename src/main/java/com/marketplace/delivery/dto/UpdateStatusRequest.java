package com.marketplace.delivery.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body to update a parcel's delivery status (admin endpoint).
 */
public record UpdateStatusRequest(

        @NotNull(message = "New status is required")
        String status,

        String notes
) {}

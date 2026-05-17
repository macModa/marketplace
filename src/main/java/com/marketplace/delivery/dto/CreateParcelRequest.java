package com.marketplace.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body for manually creating a delivery parcel.
 * Used by the POST /api/delivery/parcels endpoint.
 */
public record CreateParcelRequest(

        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotBlank(message = "Recipient name is required")
        String recipientName,

        @NotBlank(message = "Recipient phone is required")
        @Pattern(regexp = "^[0-9+]{8,15}$", message = "Invalid phone number format")
        String recipientPhone,

        @NotBlank(message = "Delivery address is required")
        String deliveryAddress,

        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "\\d{4}", message = "Tunisian postal code must be exactly 4 digits")
        String postalCode,

        @Positive(message = "Weight must be positive")
        BigDecimal weightKg
) {}

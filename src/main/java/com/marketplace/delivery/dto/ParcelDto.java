package com.marketplace.delivery.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full parcel details returned by the API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParcelDto(
        Long id,
        String trackingNumber,
        Long orderId,
        String recipientName,
        String recipientPhone,
        String deliveryAddress,
        String postalCode,
        BigDecimal weightKg,
        String status,
        String governorate,
        HubDto hub,
        RelayPointDto relayPoint,
        LocalDate estimatedDelivery,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

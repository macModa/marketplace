package com.marketplace.delivery.dto;

import java.util.List;

/**
 * Full tracking timeline response.
 */
public record TrackingResponse(
        String trackingNumber,
        String currentStatus,
        String recipientName,
        String deliveryAddress,
        String governorate,
        String estimatedDelivery,
        List<TrackingEventDto> timeline
) {}

package com.marketplace.delivery.dto;

import java.time.LocalDateTime;

/**
 * A single entry in the parcel's tracking timeline.
 */
public record TrackingEventDto(
        Long id,
        String status,
        String description,
        String location,
        LocalDateTime occurredAt
) {}

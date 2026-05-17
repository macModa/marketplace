package com.marketplace.delivery.dto;

/**
 * Hub summary included in parcel responses.
 */
public record HubDto(
        Long id,
        String name,
        String governorate,
        String address
) {}

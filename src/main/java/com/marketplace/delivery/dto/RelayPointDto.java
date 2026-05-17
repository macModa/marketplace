package com.marketplace.delivery.dto;

/**
 * Relay point info returned in parcel details and relay search results.
 */
public record RelayPointDto(
        Long id,
        String name,
        String address,
        String postalCode,
        Double latitude,
        Double longitude,
        Integer maxCapacity,
        Integer currentLoad,
        String governorate
) {}

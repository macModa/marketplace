package com.marketplace.delivery.exception;

/**
 * Thrown when a delivery resource (parcel, hub, relay point) cannot be found.
 */
public class DeliveryNotFoundException extends RuntimeException {

    public DeliveryNotFoundException(String message) {
        super(message);
    }

    public static DeliveryNotFoundException parcel(String trackingNumber) {
        return new DeliveryNotFoundException("Parcel not found with tracking number: " + trackingNumber);
    }

    public static DeliveryNotFoundException hub(String governorate) {
        return new DeliveryNotFoundException("No active hub found for governorate: " + governorate);
    }

    public static DeliveryNotFoundException relay(String postalCode) {
        return new DeliveryNotFoundException("No available relay point found for postal code: " + postalCode);
    }
}

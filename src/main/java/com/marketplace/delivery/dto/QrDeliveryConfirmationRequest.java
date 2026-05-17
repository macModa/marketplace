package com.marketplace.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QrDeliveryConfirmationRequest {
    @NotBlank
    private String trackingNumber;

    @NotBlank
    private String qrToken;      // The raw token scanned from QR

    private String deliveryNotes; // Optional notes from delivery agent

    private String gpsLocation;   // "lat,lon" from Flutter GPS
    private String deviceId;      // Delivery agent device identifier
}

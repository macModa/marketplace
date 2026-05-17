package com.marketplace.delivery.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeliveryQrResponse {
    private String trackingNumber;
    private String qrCodeBase64;   // "data:image/png;base64,..."
    private String qrToken;        // Raw token (shown once, sent to Flutter)
    private String expiresAt;      // ISO-8601
}

package com.marketplace.delivery.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class QrDeliveryConfirmationResponse {
    private boolean success;
    private String trackingNumber;
    private String newStatus;
    private Instant deliveredAt;
    private String message;
}

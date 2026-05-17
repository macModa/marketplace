package com.marketplace.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmDeliveryRequest(
    Long orderId,
    String token,
    @NotBlank String numeroSuivi
) {}

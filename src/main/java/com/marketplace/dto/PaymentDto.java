package com.marketplace.dto;

import com.marketplace.enums.PaymentMethod;
import com.marketplace.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private Long id;
    private PaymentMethod methode;
    private PaymentStatus statut;
    private String reference;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long orderId;
}


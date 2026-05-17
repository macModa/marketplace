package com.marketplace.dto;

import com.marketplace.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private OrderStatus statut;
    private BigDecimal total;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long clientId;
    private String clientNom;
    private List<OrderLineDto> orderLines = new ArrayList<>();
    private PaymentDto payment;
    private String paymentMethod;
    private String paymentStatus;
    private String deliveryToken;
    private String trackingNumber;  // Added: Tracking number from associated Parcel
}


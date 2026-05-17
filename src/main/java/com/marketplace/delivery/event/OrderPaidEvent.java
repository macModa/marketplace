package com.marketplace.delivery.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * Published by PaymentService when an order payment is successfully completed.
 *
 * Design for future Kafka migration:
 *   - Replace ApplicationEvent with a POJO
 *   - Replace @EventListener with a @KafkaListener consuming "order.paid" topic
 *   - This class becomes the Kafka message payload (serialize to JSON)
 *
 * Using package delivery.event intentionally — this event is owned by the
 * delivery bounded context even though it originates from the payment flow.
 */
public class OrderPaidEvent extends ApplicationEvent {

    private final Long orderId;
    private final String recipientName;
    private final String recipientPhone;
    private final String deliveryAddress;
    private final String postalCode;
    private final BigDecimal totalAmount;

    public OrderPaidEvent(Object source, Long orderId, String recipientName,
                          String recipientPhone, String deliveryAddress,
                          String postalCode, BigDecimal totalAmount) {
        super(source);
        this.orderId = orderId;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.deliveryAddress = deliveryAddress;
        this.postalCode = postalCode;
        this.totalAmount = totalAmount;
    }

    public Long getOrderId() { return orderId; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getPostalCode() { return postalCode; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}

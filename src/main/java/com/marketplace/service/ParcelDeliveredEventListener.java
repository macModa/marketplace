package com.marketplace.service;

import com.marketplace.delivery.event.ParcelDeliveredEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParcelDeliveredEventListener {

    private static final Logger log = LoggerFactory.getLogger(ParcelDeliveredEventListener.class);
    private final OrderService orderService;

    @EventListener
    public void handleParcelDeliveredEvent(ParcelDeliveredEvent event) {
        log.info("Received ParcelDeliveredEvent for Order ID: {}", event.getOrderId());
        orderService.completeCodPayment(event.getOrderId());
    }
}

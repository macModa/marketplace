package com.marketplace.delivery.event;

import org.springframework.context.ApplicationEvent;

public class ParcelDeliveredEvent extends ApplicationEvent {
    
    private final Long orderId;
    
    public ParcelDeliveredEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }
    
    public Long getOrderId() {
        return orderId;
    }
}

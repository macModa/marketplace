package com.marketplace.delivery.application;

import com.marketplace.delivery.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

/**
 * Listens for OrderPaidEvent and triggers automatic parcel creation.
 *
 * Architecture notes:
 *   - Uses @TransactionalEventListener(phase = AFTER_COMMIT) to ensure the parcel
 *     is created only AFTER the payment transaction commits successfully.
 *     This prevents orphan parcels from being created if the payment transaction rolls back.
 *
 *   - Kafka migration path:
 *     Replace @TransactionalEventListener with @KafkaListener(topics = "order.paid")
 *     and inject a KafkaConsumerRecord<String, OrderPaidEvent> instead.
 *     The business logic (call to deliveryService.createParcelFromOrderEvent) stays identical.
 *
 * Error handling:
 *   - Failures here are logged but do NOT rollback the payment (different transaction).
 *   - Consider adding a dead-letter queue or retry mechanism for production reliability.
 */
@Component
@RequiredArgsConstructor
public class OrderPaidEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidEventListener.class);

    private final DeliveryApplicationService deliveryService;

    /**
     * React to a completed payment and create the delivery parcel.
     *
     * TransactionPhase.AFTER_COMMIT ensures the listener runs only after
     * the payment transaction is durable in the database.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("OrderPaidEvent received for order {}. Initiating parcel creation.", event.getOrderId());

        try {
            deliveryService.createParcelFromOrderEvent(
                    event.getOrderId(),
                    event.getRecipientName(),
                    event.getRecipientPhone(),
                    event.getDeliveryAddress(),
                    event.getPostalCode(),
                    BigDecimal.ONE  // Default weight; real weight should come from order lines if available
            );
            log.info("Parcel created successfully for order {}", event.getOrderId());
        } catch (Exception e) {
            // Log and alert — do NOT rethrow (payment already committed)
            log.error("Failed to create parcel for order {}: {}", event.getOrderId(), e.getMessage(), e);
            // TODO: publish to a dead-letter queue or trigger an alert notification
        }
    }
}

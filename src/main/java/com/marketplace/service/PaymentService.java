package com.marketplace.service;

import com.marketplace.delivery.event.OrderPaidEvent;
import com.marketplace.dto.CreatePaymentRequest;
import com.marketplace.entity.Client;
import com.marketplace.entity.Order;
import com.marketplace.entity.Payment;
import com.marketplace.enums.PaymentStatus;
import com.marketplace.repository.OrderRepository;
import com.marketplace.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public Payment createPayment(Long orderId, CreatePaymentRequest request) {
        logger.info("Création d'un paiement pour la commande: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderId));
        
        if (order.getPayment() != null) {
            throw new IllegalStateException("Un paiement existe déjà pour cette commande");
        }
        
        if (order.getStatut() == com.marketplace.enums.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Impossible de payer une commande annulée");
        }
        
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethode(request.getMethode());
        payment.setStatut(PaymentStatus.PENDING);
        payment.setReference(request.getReference() != null ? request.getReference() : generateReference());
        
        Payment saved = paymentRepository.save(payment);
        logger.info("Paiement créé avec succès: ID {}, Référence: {}", saved.getId(), saved.getReference());
        
        return saved;
    }
    
    @Transactional
    public Payment completePayment(Long paymentId) {
        logger.info("Finalisation du paiement: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Paiement non trouvé: " + paymentId));
        
        payment.complete();
        
        // Update order status
        Order order = payment.getOrder();
        if (order.getStatut() == com.marketplace.enums.OrderStatus.PENDING) {
            order.setStatut(com.marketplace.enums.OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }
        
        Payment saved = paymentRepository.save(payment);
        logger.info("Paiement complété avec succès: {}", paymentId);
        
        // Publish event so DeliveryService can create the parcel automatically
        // This event is consumed AFTER this transaction commits (TransactionPhase.AFTER_COMMIT)
        publishOrderPaidEvent(order);
        
        return saved;
    }
    
    @Transactional
    public Payment failPayment(Long paymentId) {
        logger.info("Échec du paiement: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Paiement non trouvé: " + paymentId));
        
        payment.fail();
        
        return paymentRepository.save(payment);
    }
    
    @Transactional
    public Payment refundPayment(Long paymentId) {
        logger.info("Remboursement du paiement: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Paiement non trouvé: " + paymentId));
        
        payment.refund();
        
        // Cancel the order
        Order order = payment.getOrder();
        if (order.canBeCancelled()) {
            order.cancel();
            orderRepository.save(order);
        }
        
        Payment saved = paymentRepository.save(payment);
        logger.info("Paiement remboursé avec succès: {}", paymentId);
        
        return saved;
    }
    
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Paiement non trouvé pour la commande: " + orderId));
    }
    
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Paiement non trouvé: " + paymentId));
    }
    
    private String generateReference() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Build and publish OrderPaidEvent.
     * DeliveryService is NOT imported here — fully decoupled via Spring events.
     *
     * Postal code extraction: reads the last 4 digits from adresseLivraison.
     * In production, add a dedicated postalCode column to the Client entity.
     */
    private void publishOrderPaidEvent(Order order) {
        Client client = order.getClient();
        if (client == null) {
            logger.warn("Cannot publish OrderPaidEvent: no client on order {}", order.getId());
            return;
        }

        String deliveryAddress = client.getAdresseLivraison() != null
                ? client.getAdresseLivraison() : "";

        // Extract postal code: expect last 4 chars of address to be digits (e.g. "Av. Habib Bourguiba 7000")
        String postalCode = extractPostalCode(deliveryAddress);
        if (postalCode == null) {
            logger.warn("No valid 4-digit postal code found in address '{}' for order {}. " +
                    "Defaulting to 1000 (Grand Tunis). " +
                    "Consider adding a dedicated postalCode field to Client.", deliveryAddress, order.getId());
            postalCode = "1000";  // safe default — Grand Tunis
        }

        String recipientName = client.getNom();
        OrderPaidEvent event = new OrderPaidEvent(
                this,
                order.getId(),
                recipientName,
                client.getTelephone() != null ? client.getTelephone() : "",
                deliveryAddress,
                postalCode,
                order.getTotal()
        );

        eventPublisher.publishEvent(event);
        logger.info("OrderPaidEvent published for order {} (postalCode: {})", order.getId(), postalCode);
    }

    /**
     * Extract a 4-digit Tunisian postal code from an address string.
     * Looks for any sequence of exactly 4 digits in the address.
     */
    private String extractPostalCode(String address) {
        if (address == null || address.isBlank()) return null;
        // Match any standalone 4-digit group
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("\\b(\\d{4})\\b").matcher(address);
        return matcher.find() ? matcher.group(1) : null;
    }
}


package com.marketplace.service;

import com.marketplace.dto.CreatePaymentRequest;
import com.marketplace.entity.Order;
import com.marketplace.entity.Payment;
import com.marketplace.enums.PaymentStatus;
import com.marketplace.repository.OrderRepository;
import com.marketplace.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    
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
}


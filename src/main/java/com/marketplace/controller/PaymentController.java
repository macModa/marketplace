package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.CreatePaymentRequest;
import com.marketplace.entity.Payment;
import com.marketplace.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    private final PaymentService paymentService;
    
    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<Payment>> createPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Paiement créé avec succès", payment));
    }
    
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> getPaymentByOrderId(@PathVariable Long orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> getPaymentById(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }
    
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> completePayment(@PathVariable Long id) {
        Payment payment = paymentService.completePayment(id);
        return ResponseEntity.ok(ApiResponse.success("Paiement complété avec succès", payment));
    }
    
    @PutMapping("/{id}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> failPayment(@PathVariable Long id) {
        Payment payment = paymentService.failPayment(id);
        return ResponseEntity.ok(ApiResponse.success("Paiement marqué comme échoué", payment));
    }
    
    @PutMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> refundPayment(@PathVariable Long id) {
        Payment payment = paymentService.refundPayment(id);
        return ResponseEntity.ok(ApiResponse.success("Paiement remboursé avec succès", payment));
    }
}


package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.CreatePaymentRequest;
import com.marketplace.dto.PaymentDto;
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

    // =========================
    // CREATE PAYMENT
    // =========================
    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<PaymentDto>> createPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody CreatePaymentRequest request) {

        Payment payment = paymentService.createPayment(orderId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Paiement créé avec succès",
                        mapToDto(payment)
                ));
    }

    // =========================
    // GET PAYMENT BY ORDER ID
    // =========================
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto>> getPaymentByOrderId(
            @PathVariable Long orderId) {

        Payment payment = paymentService.getPaymentByOrderId(orderId);

        return ResponseEntity.ok(
                ApiResponse.success(mapToDto(payment))
        );
    }

    // =========================
    // GET PAYMENT BY ID
    // =========================
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto>> getPaymentById(
            @PathVariable Long id) {

        Payment payment = paymentService.getPaymentById(id);

        return ResponseEntity.ok(
                ApiResponse.success(mapToDto(payment))
        );
    }

    // =========================
    // COMPLETE PAYMENT
    // =========================
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto>> completePayment(
            @PathVariable Long id) {

        Payment payment = paymentService.completePayment(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Paiement complété avec succès",
                        mapToDto(payment)
                )
        );
    }

    // =========================
    // FAIL PAYMENT
    // =========================
    @PutMapping("/{id}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto>> failPayment(
            @PathVariable Long id) {

        Payment payment = paymentService.failPayment(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Paiement marqué comme échoué",
                        mapToDto(payment)
                )
        );
    }

    // =========================
    // REFUND PAYMENT
    // =========================
    @PutMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto>> refundPayment(
            @PathVariable Long id) {

        Payment payment = paymentService.refundPayment(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Paiement remboursé avec succès",
                        mapToDto(payment)
                )
        );
    }

    // =========================
    // MAPPER ENTITY -> DTO
    // =========================
    private PaymentDto mapToDto(Payment payment) {
        if (payment == null) return null;

        return new PaymentDto(
                payment.getId(),
                payment.getMethode(),
                payment.getStatut(),
                payment.getReference(),
                payment.getDateCreation(),
                payment.getDateModification(),
                payment.getOrder() != null ? payment.getOrder().getId() : null
        );
    }
}

package com.marketplace.delivery.application;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.marketplace.delivery.dto.*;
import com.marketplace.entity.Order;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryQrService {

    private final OrderRepository orderRepository;

    private static final int QR_SIZE = 400;

    // ═══════════════════════════════════════════════════════════════════════
    // 1. GENERATE QR CODE (called when status = SHIPPED)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DeliveryQrResponse generateDeliveryQr(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + trackingNumber));

        // QR content: just the trackingNumber (no token needed)
        String qrContent = trackingNumber;
        String qrBase64 = generateQrImageBase64(qrContent);

        log.info("QR generated for order {}", trackingNumber);

        return DeliveryQrResponse.builder()
                .trackingNumber(trackingNumber)
                .qrCodeBase64(qrBase64)
                .qrToken(order.getDeliveryToken()) // kept for DTO compatibility
                .expiresAt("never")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. SCAN & CONFIRM DELIVERY
    //    Flutter scans QR → extracts trackingNumber → calls this
    //    Token validation removed for MVP simplicity
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public QrDeliveryConfirmationResponse confirmDelivery(
            QrDeliveryConfirmationRequest request) {

        String trackingNumber = request.getTrackingNumber();

        // Find order by tracking number
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + trackingNumber));

        // Validate order is in SHIPPED status
        if (order.getStatut() != OrderStatus.SHIPPED) {
            throw new IllegalStateException(
                "Cannot confirm delivery: order status is " + order.getStatut() +
                " (expected SHIPPED)");
        }

        // Mark as DELIVERED
        order.setStatut(OrderStatus.DELIVERED);
        order.setDateModification(java.time.LocalDateTime.now());
        order.setDeliveryToken(null);

        orderRepository.save(order);

        log.info("Delivery confirmed via QR scan for order {}", trackingNumber);

        return QrDeliveryConfirmationResponse.builder()
                .success(true)
                .trackingNumber(trackingNumber)
                .newStatus(OrderStatus.DELIVERED.name())
                .deliveredAt(Instant.now())
                .message("Livraison confirmée avec succès")
                .build();
    }

    @Transactional
    public void invalidateToken(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + trackingNumber));
        order.setDeliveryToken(null);
        orderRepository.save(order);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Private Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private String generateQrImageBase64(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            java.util.HashMap<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(com.google.zxing.EncodeHintType.MARGIN, 2);

            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException("QR generation failed", e);
        }
    }
}

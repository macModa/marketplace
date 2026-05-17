package com.marketplace.delivery.api;

import com.marketplace.delivery.application.DeliveryQrService;
import com.marketplace.delivery.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/delivery/qr")
@RequiredArgsConstructor
public class DeliveryQrController {

    private final DeliveryQrService qrService;

    /**
     * Generate a delivery confirmation QR code.
     * Called by admin/driver app when marking parcel as OUT_FOR_DELIVERY.
     */
    @GetMapping("/{trackingNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER', 'ARTISAN')")
    public ResponseEntity<DeliveryQrResponse> generateQr(
            @PathVariable String trackingNumber) {
        return ResponseEntity.ok(qrService.generateDeliveryQr(trackingNumber));
    }

    /**
     * Confirm delivery by scanning QR code.
     * Called by Flutter delivery agent app.
     */
    @PostMapping("/confirm")
    public ResponseEntity<QrDeliveryConfirmationResponse> confirmDelivery(
            @Valid @RequestBody QrDeliveryConfirmationRequest request) {
        return ResponseEntity.ok(qrService.confirmDelivery(request));
    }

    /**
     * Regenerate QR if expired or lost (admin only).
     */
    @PostMapping("/regenerate/{trackingNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryQrResponse> regenerateQr(
            @PathVariable String trackingNumber) {
        qrService.invalidateToken(trackingNumber);
        return ResponseEntity.ok(qrService.generateDeliveryQr(trackingNumber));
    }
}

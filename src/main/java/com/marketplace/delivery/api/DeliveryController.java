package com.marketplace.delivery.api;

import com.marketplace.delivery.application.DeliveryApplicationService;
import com.marketplace.delivery.application.TrackingService;
import com.marketplace.delivery.domain.DeliveryStatus;
import com.marketplace.delivery.domain.Parcel;
import com.marketplace.delivery.domain.RelayPoint;
import com.marketplace.delivery.domain.TrackingEvent;
import com.marketplace.delivery.dto.*;
import com.marketplace.delivery.mapper.DeliveryMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for the delivery module.
 *
 * Endpoints:
 *   POST   /api/delivery/parcels                        → create parcel manually
 *   GET    /api/delivery/parcels/{trackingNumber}        → get parcel details
 *   GET    /api/delivery/relays?postalCode=XXXX          → list relays for a postal code
 *   GET    /api/delivery/tracking/{trackingNumber}       → full tracking timeline
 *   PUT    /api/delivery/parcels/{trackingNumber}/status → update status (admin)
 *
 * Future: add @PreAuthorize("hasRole('ADMIN')") on write endpoints once
 * the delivery module has its own security context.
 */
@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryController.class);

    private final DeliveryApplicationService deliveryService;
    private final TrackingService trackingService;
    private final DeliveryMapper deliveryMapper;

    // ─── POST /api/delivery/parcels ───────────────────────────────────────────

    /**
     * Manually create a delivery parcel for a paid order.
     * In normal flow, parcels are created automatically via OrderPaidEvent.
     * This endpoint is for admin/operator use or retry scenarios.
     */
    @PostMapping("/parcels")
    public ResponseEntity<ParcelDto> createParcel(@Valid @RequestBody CreateParcelRequest request) {
        log.info("Manual parcel creation request for order {}", request.orderId());

        Parcel parcel = deliveryService.createParcelManually(
                request.orderId(),
                request.recipientName(),
                request.recipientPhone(),
                request.deliveryAddress(),
                request.postalCode(),
                request.weightKg()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deliveryMapper.toParcelDto(parcel));
    }

    // ─── GET /api/delivery/parcels/{trackingNumber} ───────────────────────────

    /**
     * Get full parcel details by tracking number.
     */
    @GetMapping("/parcels/{trackingNumber}")
    public ResponseEntity<ParcelDto> getParcel(@PathVariable String trackingNumber) {
        log.debug("Fetching parcel details for {}", trackingNumber);
        Parcel parcel = deliveryService.getByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(deliveryMapper.toParcelDto(parcel));
    }

    // ─── GET /api/delivery/relays?postalCode=XXXX ─────────────────────────────

    /**
     * List active relay points that serve a given postal code.
     * Falls back to governorate-wide results if no exact match exists.
     */
    @GetMapping("/relays")
    public ResponseEntity<List<RelayPointDto>> getRelaysForPostalCode(
            @RequestParam String postalCode) {
        log.debug("Relay point query for postal code {}", postalCode);
        List<RelayPoint> relays = deliveryService.getRelaysForPostalCode(postalCode);
        return ResponseEntity.ok(deliveryMapper.toRelayPointDtoList(relays));
    }

    // ─── GET /api/delivery/tracking/{trackingNumber} ──────────────────────────

    /**
     * Get the full chronological tracking timeline for a parcel.
     */
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<TrackingResponse> getTracking(@PathVariable String trackingNumber) {
        log.debug("Tracking timeline requested for {}", trackingNumber);
        Parcel parcel = deliveryService.getByTrackingNumber(trackingNumber);
        List<TrackingEvent> events = trackingService.getHistory(trackingNumber);
        return ResponseEntity.ok(deliveryMapper.toTrackingResponse(parcel, events));
    }

    // ─── PUT /api/delivery/parcels/{trackingNumber}/status ────────────────────

    /**
     * Update parcel delivery status. Admin/operator use only.
     * Validates status string against the DeliveryStatus enum.
     */
    @PutMapping("/parcels/{trackingNumber}/status")
    public ResponseEntity<ParcelDto> updateStatus(
            @PathVariable String trackingNumber,
            @Valid @RequestBody UpdateStatusRequest request) {

        DeliveryStatus newStatus;
        try {
            newStatus = DeliveryStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid delivery status: " + request.status());
        }

        log.info("Status update request: parcel={}, newStatus={}", trackingNumber, newStatus);
        Parcel updated = deliveryService.updateStatus(trackingNumber, newStatus, request.notes());
        return ResponseEntity.ok(deliveryMapper.toParcelDto(updated));
    }
}

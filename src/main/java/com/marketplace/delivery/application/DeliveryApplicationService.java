package com.marketplace.delivery.application;

import com.marketplace.delivery.domain.*;
import com.marketplace.delivery.exception.DeliveryNotFoundException;
import com.marketplace.delivery.infrastructure.ParcelRepository;
import com.marketplace.delivery.infrastructure.RelayPointRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Central orchestrator for the delivery domain.
 *
 * End-to-end parcel creation flow:
 *   1. Validate inputs
 *   2. Generate unique tracking number
 *   3. Detect governorate via postal code prefix (GovernorateDetector)
 *   4. Assign regional hub (HubAssignmentService)
 *   5. Select best relay point (RelaySelectionService → SmartRelaySelectionStrategy)
 *   6. Save parcel record
 *   7. Create initial CREATED tracking event (TrackingService)
 *
 * This class does NOT depend on OrderService — it receives data via the
 * OrderPaidEvent (or directly via the REST API for manual creation).
 */
@Service
@RequiredArgsConstructor
public class DeliveryApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryApplicationService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int ESTIMATED_DELIVERY_DAYS = 3;

    private final ParcelRepository parcelRepository;
    private final RelayPointRepository relayPointRepository;
    private final GovernorateDetector governorateDetector;
    private final HubAssignmentService hubAssignmentService;
    private final RelaySelectionService relaySelectionService;
    private final TrackingService trackingService;
    private final ApplicationEventPublisher eventPublisher;
    private final DeliveryQrService deliveryQrService;

    // ─── Parcel Creation ──────────────────────────────────────────────────────

    /**
     * Create a Parcel from the data published by OrderPaidEvent.
     * Called by OrderPaidEventListener — fully decoupled from PaymentService.
     */
    @Transactional
    public Parcel createParcelFromOrderEvent(Long orderId, String recipientName,
                                              String recipientPhone, String deliveryAddress,
                                              String postalCode, BigDecimal weightKg) {
        log.info("Creating parcel for order {} → postal code {}", orderId, postalCode);

        return buildAndSaveParcel(orderId, recipientName, recipientPhone,
                deliveryAddress, postalCode, weightKg);
    }

    /**
     * Create a Parcel via the REST API (manual creation by admin/operator).
     */
    @Transactional
    public Parcel createParcelManually(Long orderId, String recipientName,
                                        String recipientPhone, String deliveryAddress,
                                        String postalCode, BigDecimal weightKg) {
        log.info("Manual parcel creation requested for order {}", orderId);

        // Prevent duplicate parcels for the same order
        if (parcelRepository.existsByOrderId(orderId)) {
            throw new IllegalStateException("A parcel already exists for order ID: " + orderId);
        }

        return buildAndSaveParcel(orderId, recipientName, recipientPhone,
                deliveryAddress, postalCode, weightKg);
    }

    // ─── Query ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Parcel getByTrackingNumber(String trackingNumber) {
        return parcelRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> DeliveryNotFoundException.parcel(trackingNumber));
    }

    @Transactional(readOnly = true)
    public List<RelayPoint> getRelaysForPostalCode(String postalCode) {
        List<RelayPoint> relays = relayPointRepository.findByPostalCodeAndActiveTrue(postalCode);
        if (relays.isEmpty()) {
            Governorate gov = governorateDetector.detect(postalCode);
            relays = relayPointRepository.findByHub_GovernorateAndActiveTrue(gov);
        }
        return relays;
    }

    // ─── Status Update ────────────────────────────────────────────────────────

    /**
     * Update parcel delivery status. Called from the admin controller endpoint.
     * Also releases relay point load when parcel is DELIVERED or RETURNED.
     */
    @Transactional
    public Parcel updateStatus(String trackingNumber, DeliveryStatus newStatus, String notes) {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> DeliveryNotFoundException.parcel(trackingNumber));

        if (parcel.isFinalState()) {
            throw new IllegalStateException("Parcel " + trackingNumber + " is already in a final state: " + parcel.getStatus());
        }

        DeliveryStatus oldStatus = parcel.getStatus();
        parcel.updateStatus(newStatus);
        parcelRepository.save(parcel);

        // Release relay load when parcel leaves the relay (delivered or returned)
        if ((newStatus == DeliveryStatus.DELIVERED || newStatus == DeliveryStatus.RETURNED)
                && parcel.getRelayPoint() != null) {
            parcel.getRelayPoint().decrementLoad();
            relayPointRepository.save(parcel.getRelayPoint());
        }
        
        if (newStatus == DeliveryStatus.DELIVERED && parcel.getOrderId() != null) {
            eventPublisher.publishEvent(new com.marketplace.delivery.event.ParcelDeliveredEvent(this, parcel.getOrderId()));
            log.info("Published ParcelDeliveredEvent for Order ID: {}", parcel.getOrderId());
        }

        // When marking as OUT_FOR_DELIVERY, auto-generate QR token
        if (newStatus == DeliveryStatus.OUT_FOR_DELIVERY) {
            try {
                deliveryQrService.generateDeliveryQr(trackingNumber);
                log.info("Auto-generated delivery QR for parcel {}", trackingNumber);
            } catch (Exception e) {
                log.warn("Failed to auto-generate QR for {}: {}", trackingNumber, e.getMessage());
                // Non-blocking: delivery can proceed without QR if needed
            }
        }

        String description = buildStatusDescription(newStatus, notes);
        String location = resolveLocation(parcel, newStatus);
        trackingService.recordEvent(trackingNumber, newStatus, description, location);

        log.info("Parcel {} status updated: {} → {}", trackingNumber, oldStatus, newStatus);
        return parcel;
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private Parcel buildAndSaveParcel(Long orderId, String recipientName, String recipientPhone,
                                       String deliveryAddress, String postalCode, BigDecimal weightKg) {
        // Step 1: detect governorate
        Governorate governorate = governorateDetector.detect(postalCode);

        // Step 2: assign hub
        Hub hub = hubAssignmentService.assignHub(governorate);

        // Step 3: build parcel stub (needed for relay selection to read postalCode / trackingNumber)
        String trackingNumber = generateTrackingNumber();

        Parcel parcel = new Parcel();
        parcel.setTrackingNumber(trackingNumber);
        parcel.setOrderId(orderId);
        parcel.setRecipientName(recipientName);
        parcel.setRecipientPhone(recipientPhone);
        parcel.setDeliveryAddress(deliveryAddress);
        parcel.setPostalCode(postalCode);
        parcel.setWeightKg(weightKg != null ? weightKg : BigDecimal.ONE);
        parcel.setHub(hub);
        parcel.setStatus(DeliveryStatus.CREATED);
        parcel.setEstimatedDelivery(LocalDate.now().plusDays(ESTIMATED_DELIVERY_DAYS));

        // Step 4: select relay (increments relay load internally)
        RelayPoint relay = relaySelectionService.selectRelay(parcel, governorate);
        parcel.setRelayPoint(relay);
        parcel.setStatus(DeliveryStatus.ASSIGNED_TO_HUB);

        // Step 5: persist
        Parcel saved = parcelRepository.save(parcel);
        log.info("Parcel {} saved: hub={}, relay={}, governorate={}",
                trackingNumber, hub.getName(), relay.getName(), governorate.getDisplayName());

        // Step 6: create initial tracking event
        trackingService.recordEvent(trackingNumber, DeliveryStatus.ASSIGNED_TO_HUB,
                "Parcel created and assigned to " + hub.getName(),
                hub.getName());

        return saved;
    }

    /**
     * Generate a unique tracking number in format DLV-YYYYMMDD-XXXXXXXX.
     * Collision probability is negligible for expected traffic volumes.
     * Future: replace with a dedicated sequence generator for high-volume scenarios.
     */
    private String generateTrackingNumber() {
        String date = LocalDateTime.now().format(DATE_FMT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "DLV-" + date + "-" + suffix;
    }

    private String buildStatusDescription(DeliveryStatus status, String notes) {
        String base = switch (status) {
            case CREATED        -> "Parcel created and registered.";
            case ASSIGNED_TO_HUB -> "Parcel assigned to regional hub.";
            case SORTED         -> "Parcel sorted at the hub.";
            case IN_TRANSIT     -> "Parcel in transit to relay point.";
            case ARRIVED_AT_RELAY -> "Parcel arrived at relay point.";
            case OUT_FOR_DELIVERY -> "Parcel out for last-mile delivery.";
            case DELIVERED      -> "Parcel delivered successfully.";
            case FAILED         -> "Delivery attempt failed.";
            case RETURNED       -> "Parcel returned to sender.";
        };
        return notes != null && !notes.isBlank() ? base + " " + notes : base;
    }

    private String resolveLocation(Parcel parcel, DeliveryStatus status) {
        return switch (status) {
            case ASSIGNED_TO_HUB, SORTED -> parcel.getHub() != null ? parcel.getHub().getName() : "Hub";
            case ARRIVED_AT_RELAY, OUT_FOR_DELIVERY, DELIVERED ->
                    parcel.getRelayPoint() != null ? parcel.getRelayPoint().getName() : "Relay Point";
            default -> parcel.getDeliveryAddress();
        };
    }
}

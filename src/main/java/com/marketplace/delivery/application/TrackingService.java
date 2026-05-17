package com.marketplace.delivery.application;

import com.marketplace.delivery.domain.DeliveryStatus;
import com.marketplace.delivery.domain.Parcel;
import com.marketplace.delivery.domain.TrackingEvent;
import com.marketplace.delivery.exception.DeliveryNotFoundException;
import com.marketplace.delivery.infrastructure.ParcelRepository;
import com.marketplace.delivery.infrastructure.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the tracking lifecycle of a parcel.
 *
 * Responsibilities:
 *   - Record tracking events (immutable audit trail)
 *   - Retrieve the full event history for a tracking number
 *   - Transition parcel status
 *
 * Future: publish each status change to Kafka topic "delivery.tracking.events"
 * so mobile apps and external partners receive real-time updates.
 */
@Service
@RequiredArgsConstructor
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final ParcelRepository parcelRepository;
    private final TrackingEventRepository trackingEventRepository;

    /**
     * Record a new tracking event and update the parcel status atomically.
     *
     * @param trackingNumber Parcel tracking number.
     * @param newStatus      The new delivery status.
     * @param description    Human-readable description of the event.
     * @param location       Physical location where the event occurred.
     * @return The saved TrackingEvent.
     */
    @Transactional
    public TrackingEvent recordEvent(String trackingNumber, DeliveryStatus newStatus,
                                     String description, String location) {
        Parcel parcel = findParcelOrThrow(trackingNumber);

        // Update parcel status
        parcel.updateStatus(newStatus);
        parcelRepository.save(parcel);

        // Create immutable event record
        TrackingEvent event = new TrackingEvent();
        event.setParcel(parcel);
        event.setStatus(newStatus);
        event.setDescription(description);
        event.setLocation(location);

        TrackingEvent saved = trackingEventRepository.save(event);
        log.info("Tracking event recorded: parcel={}, status={}, location={}",
                trackingNumber, newStatus, location);

        return saved;
    }

    /**
     * Get the full chronological tracking history for a parcel.
     *
     * @param trackingNumber Parcel tracking number.
     * @return List of events ordered by occurrence time (ascending).
     */
    @Transactional(readOnly = true)
    public List<TrackingEvent> getHistory(String trackingNumber) {
        Parcel parcel = findParcelOrThrow(trackingNumber);
        return trackingEventRepository.findByParcelOrderByOccurredAtAsc(parcel);
    }

    /**
     * Helper: look up parcel or throw a meaningful 404 exception.
     */
    private Parcel findParcelOrThrow(String trackingNumber) {
        return parcelRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> DeliveryNotFoundException.parcel(trackingNumber));
    }
}

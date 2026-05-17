package com.marketplace.delivery.mapper;

import com.marketplace.delivery.domain.*;
import com.marketplace.delivery.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manual DTO mapper — no annotation processor dependency.
 * Pure Java, fully testable, zero magic.
 */
@Component
public class DeliveryMapper {

    // ─── Parcel ──────────────────────────────────────────────────────────────

    public ParcelDto toParcelDto(Parcel parcel) {
        if (parcel == null) return null;

        String governorate = null;
        if (parcel.getHub() != null) {
            governorate = parcel.getHub().getGovernorate().getDisplayName();
        }

        return new ParcelDto(
                parcel.getId(),
                parcel.getTrackingNumber(),
                parcel.getOrderId(),
                parcel.getRecipientName(),
                parcel.getRecipientPhone(),
                parcel.getDeliveryAddress(),
                parcel.getPostalCode(),
                parcel.getWeightKg(),
                parcel.getStatus().name(),
                governorate,
                toHubDto(parcel.getHub()),
                toRelayPointDto(parcel.getRelayPoint()),
                parcel.getEstimatedDelivery(),
                parcel.getCreatedAt(),
                parcel.getUpdatedAt()
        );
    }

    // ─── Hub ─────────────────────────────────────────────────────────────────

    public HubDto toHubDto(Hub hub) {
        if (hub == null) return null;
        return new HubDto(
                hub.getId(),
                hub.getName(),
                hub.getGovernorate().getDisplayName(),
                hub.getAddress()
        );
    }

    // ─── RelayPoint ───────────────────────────────────────────────────────────

    public RelayPointDto toRelayPointDto(RelayPoint relay) {
        if (relay == null) return null;
        return new RelayPointDto(
                relay.getId(),
                relay.getName(),
                relay.getAddress(),
                relay.getPostalCode(),
                relay.getLatitude(),
                relay.getLongitude(),
                relay.getMaxCapacity(),
                relay.getCurrentLoad(),
                relay.getHub() != null ? relay.getHub().getGovernorate().getDisplayName() : null
        );
    }

    public List<RelayPointDto> toRelayPointDtoList(List<RelayPoint> relays) {
        return relays.stream().map(this::toRelayPointDto).toList();
    }

    // ─── TrackingEvent ────────────────────────────────────────────────────────

    public TrackingEventDto toTrackingEventDto(TrackingEvent event) {
        if (event == null) return null;
        return new TrackingEventDto(
                event.getId(),
                event.getStatus().name(),
                event.getDescription(),
                event.getLocation(),
                event.getOccurredAt()
        );
    }

    public List<TrackingEventDto> toTrackingEventDtoList(List<TrackingEvent> events) {
        return events.stream().map(this::toTrackingEventDto).toList();
    }

    // ─── Tracking Response ────────────────────────────────────────────────────

    public TrackingResponse toTrackingResponse(Parcel parcel, List<TrackingEvent> events) {
        String governorate = parcel.getHub() != null
                ? parcel.getHub().getGovernorate().getDisplayName()
                : "Unknown";
        String estimated = parcel.getEstimatedDelivery() != null
                ? parcel.getEstimatedDelivery().toString()
                : "N/A";

        return new TrackingResponse(
                parcel.getTrackingNumber(),
                parcel.getStatus().name(),
                parcel.getRecipientName(),
                parcel.getDeliveryAddress(),
                governorate,
                estimated,
                toTrackingEventDtoList(events)
        );
    }
}

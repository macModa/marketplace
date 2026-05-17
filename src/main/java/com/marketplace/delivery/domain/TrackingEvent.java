package com.marketplace.delivery.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single event in the parcel's tracking history.
 * Forms an immutable audit trail of the parcel's journey.
 *
 * Future: can be published to Kafka as a delivery-event topic for real-time tracking.
 */
@Entity
@Table(name = "delivery_tracking_events", indexes = {
        @Index(name = "idx_tracking_parcel", columnList = "parcel_id"),
        @Index(name = "idx_tracking_status", columnList = "status"),
        @Index(name = "idx_tracking_occurred_at", columnList = "occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status;

    /** Human-readable description of the event (e.g., "Parcel arrived at Tunis Hub"). */
    @Column(nullable = false, length = 500)
    private String description;

    /** Physical location where the event occurred (e.g., hub name, relay name). */
    @Column(length = 200)
    private String location;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();
}

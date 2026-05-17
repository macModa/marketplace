package com.marketplace.delivery.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core aggregate root of the delivery domain.
 *
 * A Parcel is created automatically when an Order is paid.
 * It references an Order by orderId (value, not JPA relation) to enable future
 * decoupling into a separate microservice without cross-module entity joins.
 *
 * Indexing strategy:
 *  - tracking_number: primary lookup index (unique)
 *  - status: used for operational dashboards and batch queries
 *  - postal_code: supports future table partitioning by postal prefix
 *  - order_id: for reverse lookup (order → parcel)
 */
@Entity
@Table(name = "delivery_parcels", indexes = {
        @Index(name = "idx_parcel_tracking", columnList = "tracking_number", unique = true),
        @Index(name = "idx_parcel_status", columnList = "status"),
        @Index(name = "idx_parcel_postal", columnList = "postal_code"),
        @Index(name = "idx_parcel_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable unique tracking number.
     * Format: DLV-YYYYMMDD-XXXXXXXX
     */
    @Column(name = "tracking_number", nullable = false, unique = true, length = 30)
    private String trackingNumber;

    /**
     * Loose reference to the Order. Stored as a plain Long (not a JPA @ManyToOne)
     * so this entity can be extracted to a microservice without cross-service joins.
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    // ─── Recipient Info ──────────────────────────────────────────────────────

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "delivery_address", nullable = false, length = 300)
    private String deliveryAddress;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    // ─── Parcel Physical Info ─────────────────────────────────────────────────

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    // ─── Routing ─────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id")
    private Hub hub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relay_point_id")
    private RelayPoint relayPoint;

    // ─── Status & Dates ──────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status = DeliveryStatus.CREATED;

    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Tracking Events ─────────────────────────────────────────────────────

    @OneToMany(mappedBy = "parcel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("occurredAt ASC")
    private List<TrackingEvent> trackingEvents = new ArrayList<>();

    // ─── Business Methods ─────────────────────────────────────────────────────

    /**
     * Transition parcel to a new status. Validates allowed transitions.
     */
    public void updateStatus(DeliveryStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDelivered() {
        return this.status == DeliveryStatus.DELIVERED;
    }

    public boolean isFinalState() {
        return this.status == DeliveryStatus.DELIVERED
                || this.status == DeliveryStatus.RETURNED;
    }
}

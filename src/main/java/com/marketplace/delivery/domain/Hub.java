package com.marketplace.delivery.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A regional sorting and distribution hub.
 * Each hub serves one Governorate and manages multiple relay points.
 *
 * Future: Hub capacity analytics, route optimization between hubs.
 */
@Entity
@Table(name = "delivery_hubs", indexes = {
        @Index(name = "idx_hub_governorate", columnList = "governorate"),
        @Index(name = "idx_hub_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Governorate governorate;

    @Column(nullable = false, length = 255)
    private String address;

    /** Latitude for future distance-based routing. */
    @Column(nullable = false)
    private Double latitude;

    /** Longitude for future distance-based routing. */
    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "hub", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RelayPoint> relayPoints = new ArrayList<>();

    // ─── Business methods ─────────────────────────────────────────────────────

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}

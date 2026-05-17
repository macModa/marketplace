package com.marketplace.delivery.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A physical relay point (mini pickup/dropoff location) where parcels are stored
 * pending last-mile delivery or client pickup.
 *
 * The scoring algorithm uses latitude/longitude, maxCapacity and currentLoad
 * to select the best relay.
 *
 * Future: integrate with a geo-distance API or Haversine formula for real distances.
 */
@Entity
@Table(name = "delivery_relay_points", indexes = {
        @Index(name = "idx_relay_postal_code", columnList = "postal_code"),
        @Index(name = "idx_relay_hub", columnList = "hub_id"),
        @Index(name = "idx_relay_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RelayPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    /**
     * Maximum number of parcels this relay point can hold simultaneously.
     */
    @Column(nullable = false)
    private Integer maxCapacity;

    /**
     * Current number of parcels stored at this relay point.
     * Updated whenever a parcel is assigned, picked up, or delivered.
     */
    @Column(nullable = false)
    private Integer currentLoad = 0;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    // ─── Business methods ─────────────────────────────────────────────────────

    /**
     * @return ratio of current load vs max capacity (0.0 to 1.0)
     */
    public double getCurrentLoadRatio() {
        if (maxCapacity == 0) return 1.0;
        return (double) currentLoad / maxCapacity;
    }

    /**
     * @return ratio of available capacity vs max capacity (0.0 to 1.0)
     */
    public double getAvailableCapacityRatio() {
        if (maxCapacity == 0) return 0.0;
        return (double) (maxCapacity - currentLoad) / maxCapacity;
    }

    /**
     * @return true if the relay point can accept at least one more parcel
     */
    public boolean hasAvailableCapacity() {
        return currentLoad < maxCapacity;
    }

    public void incrementLoad() {
        this.currentLoad++;
    }

    public void decrementLoad() {
        if (this.currentLoad > 0) {
            this.currentLoad--;
        }
    }
}

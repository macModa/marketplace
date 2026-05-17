package com.marketplace.delivery.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps a Tunisian 4-digit postal code to a Governorate and zone name.
 *
 * Designed for Redis caching in a future microservice architecture:
 * this table will be the source-of-truth for a cache-aside pattern.
 *
 * Indexed on postal_code for fast lookup at parcel creation time.
 * Partitionable by postal prefix for horizontal scaling.
 */
@Entity
@Table(name = "delivery_postal_zones", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_postal_zone_name", columnNames = {"postal_code", "zone_name"})
    },
    indexes = {
        @Index(name = "idx_postal_zone_code", columnList = "postal_code"),
        @Index(name = "idx_postal_zone_governorate", columnList = "governorate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostalZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "postal_code", nullable = false, length = 4)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Governorate governorate;

    /** Human-readable name of the zone (e.g., "La Marsa", "Bizerte Ville"). */
    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;
}

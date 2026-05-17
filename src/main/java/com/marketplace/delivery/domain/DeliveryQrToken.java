package com.marketplace.delivery.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Immutable QR token for delivery confirmation.
 * One-time use token with expiration.
 */
@Entity
@Table(name = "delivery_qr_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DeliveryQrToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String trackingNumber;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;  // SHA-256 of the raw token

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private Instant createdAt;

    @Version
    private Long version;  // Optimistic locking for concurrent scans

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void markAsUsed() {
        this.used = true;
    }
}

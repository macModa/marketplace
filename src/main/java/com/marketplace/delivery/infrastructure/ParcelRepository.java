package com.marketplace.delivery.infrastructure;

import com.marketplace.delivery.domain.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Parcel persistence.
 *
 * Future Redis caching layer (cache-aside pattern):
 *   Wrap with a CachingParcelRepository that checks Redis
 *   before delegating to this JPA repository.
 */
@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {

    Optional<Parcel> findByTrackingNumber(String trackingNumber);

    boolean existsByOrderId(Long orderId);

    Optional<Parcel> findByOrderId(Long orderId);
}

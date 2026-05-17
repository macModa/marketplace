package com.marketplace.delivery.infrastructure;

import com.marketplace.delivery.domain.Governorate;
import com.marketplace.delivery.domain.RelayPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelayPointRepository extends JpaRepository<RelayPoint, Long> {

    /** Find active relay points serving a specific postal code. */
    List<RelayPoint> findByPostalCodeAndActiveTrue(String postalCode);

    /** Fallback: find all active relay points in a governorate (via hub). */
    List<RelayPoint> findByHub_GovernorateAndActiveTrue(Governorate governorate);

    /** For admin dashboard — all relays regardless of status. */
    List<RelayPoint> findByHub_Governorate(Governorate governorate);
}

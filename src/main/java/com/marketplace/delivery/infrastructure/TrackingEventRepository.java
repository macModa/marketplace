package com.marketplace.delivery.infrastructure;

import com.marketplace.delivery.domain.Parcel;
import com.marketplace.delivery.domain.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    /** Return full event history for a parcel in chronological order. */
    List<TrackingEvent> findByParcelOrderByOccurredAtAsc(Parcel parcel);
}

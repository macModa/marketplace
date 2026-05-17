package com.marketplace.delivery.application;

import com.marketplace.delivery.domain.Parcel;
import com.marketplace.delivery.domain.RelayPoint;

import java.util.List;

/**
 * Strategy interface for relay point selection.
 *
 * Separation of concerns:
 *   - Business logic (which relay to pick) is encapsulated here.
 *   - DeliveryService / RelaySelectionService just call this interface.
 *
 * Extension points:
 *   1. Swap SmartRelaySelectionStrategy for an MLRelaySelectionStrategy
 *      that calls an external REST ML service.
 *   2. Add A/B testing by routing to different strategies based on a flag.
 *   3. Add a CachedRelaySelectionStrategy wrapping another strategy with Redis.
 */
public interface RelaySelectionStrategy {

    /**
     * Select the most suitable relay point from a list of candidates.
     *
     * @param candidates List of active relay points with available capacity.
     *                   Guaranteed to be non-null and non-empty.
     * @param parcel     The parcel to be routed (provides postal code, weight, etc.)
     * @return The selected relay point.
     */
    RelayPoint selectRelay(List<RelayPoint> candidates, Parcel parcel);
}

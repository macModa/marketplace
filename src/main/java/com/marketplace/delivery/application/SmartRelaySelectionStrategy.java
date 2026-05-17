package com.marketplace.delivery.application;

import com.marketplace.delivery.domain.Parcel;
import com.marketplace.delivery.domain.RelayPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Smart relay selection based on a weighted scoring algorithm.
 *
 * Score formula (lower is better):
 *
 *   Score = (distanceKm × 0.5) + (currentLoadRatio × 0.3) − (availableCapacityRatio × 0.2)
 *
 * Weights rationale:
 *   - Distance (0.5): highest priority — minimize travel time/cost.
 *   - Current load (0.3): avoid overloaded points to prevent delays.
 *   - Available capacity (−0.2): reward points with more room (negative → lower score = better).
 *
 * Notes:
 *   - Pre-filters relays with no available capacity.
 *   - Distance is approximated using Haversine formula from relay lat/lon to a
 *     hub centroid (hub's lat/lon). In a real deployment, replace with a
 *     distances API call or precomputed distance matrix.
 *   - When totalAmount of distance data improves, this class can delegate to an
 *     ML REST endpoint without changing any other code.
 */
@Component
public class SmartRelaySelectionStrategy implements RelaySelectionStrategy {

    private static final Logger log = LoggerFactory.getLogger(SmartRelaySelectionStrategy.class);

    private static final double WEIGHT_DISTANCE = 0.5;
    private static final double WEIGHT_LOAD      = 0.3;
    private static final double WEIGHT_CAPACITY  = 0.2;

    @Override
    public RelayPoint selectRelay(List<RelayPoint> candidates, Parcel parcel) {
        log.debug("Scoring {} relay candidates for parcel {} (postal: {})",
                candidates.size(), parcel.getTrackingNumber(), parcel.getPostalCode());

        RelayPoint best = candidates.stream()
                .filter(RelayPoint::hasAvailableCapacity)
                .min(Comparator.comparingDouble(relay -> computeScore(relay, parcel)))
                .orElseThrow(() -> new IllegalStateException(
                        "All relay points are at full capacity for postal code: " + parcel.getPostalCode()));

        log.info("Selected relay '{}' (score: {:.3f}) for parcel {}",
                best.getName(),
                computeScore(best, parcel),
                parcel.getTrackingNumber());

        return best;
    }

    /**
     * Compute the routing score for a given relay point.
     * Lower score = better choice.
     */
    private double computeScore(RelayPoint relay, Parcel parcel) {
        double distanceKm = estimateDistance(relay, parcel);
        double loadRatio = relay.getCurrentLoadRatio();
        double capacityRatio = relay.getAvailableCapacityRatio();

        double score = (distanceKm * WEIGHT_DISTANCE)
                + (loadRatio * WEIGHT_LOAD)
                - (capacityRatio * WEIGHT_CAPACITY);

        log.debug("Relay '{}': distKm={:.2f}, loadRatio={:.2f}, capRatio={:.2f} → score={:.4f}",
                relay.getName(), distanceKm, loadRatio, capacityRatio, score);

        return score;
    }

    /**
     * Estimate distance from relay to its hub using Haversine formula.
     *
     * This acts as a proxy for the actual delivery distance.
     * Replace this method with a real geo-distance API call (e.g., OSRM, Google Maps)
     * or a precomputed distance matrix for production accuracy.
     *
     * @return distance in kilometers
     */
    private double estimateDistance(RelayPoint relay, Parcel parcel) {
        if (relay.getHub() == null) return 0.0;

        double hubLat = relay.getHub().getLatitude();
        double hubLon = relay.getHub().getLongitude();
        double relayLat = relay.getLatitude();
        double relayLon = relay.getLongitude();

        return haversineKm(hubLat, hubLon, relayLat, relayLon);
    }

    /**
     * Haversine formula to compute the great-circle distance between two points.
     * Accurate to ~0.3% for distances up to a few hundred km.
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

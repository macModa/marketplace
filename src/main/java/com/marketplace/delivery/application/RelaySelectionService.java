package com.marketplace.delivery.application;

import com.marketplace.delivery.domain.Governorate;
import com.marketplace.delivery.domain.Parcel;
import com.marketplace.delivery.domain.RelayPoint;
import com.marketplace.delivery.exception.DeliveryNotFoundException;
import com.marketplace.delivery.infrastructure.RelayPointRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates relay point selection for a parcel.
 *
 * Flow:
 *  1. Query active relay points that serve the parcel's postal code or governorate.
 *  2. Validate at least one candidate exists.
 *  3. Delegate to RelaySelectionStrategy for scoring and final selection.
 *  4. Increment the selected relay's load counter.
 *
 * Future ML integration:
 *   - Swap SmartRelaySelectionStrategy for MLRelaySelectionStrategy
 *   - MLRelaySelectionStrategy calls REST endpoint: POST /ml/routing/score
 *   - No changes needed here — just swap the injected strategy bean.
 */
@Service
@RequiredArgsConstructor
public class RelaySelectionService {

    private static final Logger log = LoggerFactory.getLogger(RelaySelectionService.class);

    private final RelayPointRepository relayPointRepository;
    private final RelaySelectionStrategy relaySelectionStrategy;

    /**
     * Select the best relay point for the given parcel.
     * First attempts to find relays by exact postal code, then falls back to governorate-wide search.
     *
     * @param parcel      The parcel (must have postalCode and hub set).
     * @param governorate The detected governorate.
     * @return The selected RelayPoint with incremented load.
     */
    public RelayPoint selectRelay(Parcel parcel, Governorate governorate) {
        String postalCode = parcel.getPostalCode();
        log.info("Selecting relay for parcel {} with postal code {}", parcel.getTrackingNumber(), postalCode);

        // First: look for relays matching the exact postal code
        List<RelayPoint> candidates = relayPointRepository.findByPostalCodeAndActiveTrue(postalCode);

        // Fallback: expand search to entire governorate
        if (candidates.isEmpty()) {
            log.debug("No relay found for postal code {}. Expanding to governorate: {}", postalCode, governorate);
            candidates = relayPointRepository.findByHub_GovernorateAndActiveTrue(governorate);
        }

        if (candidates.isEmpty()) {
            throw DeliveryNotFoundException.relay(postalCode);
        }

        log.debug("Found {} relay candidates for postal code {}", candidates.size(), postalCode);

        RelayPoint selected = relaySelectionStrategy.selectRelay(candidates, parcel);
        selected.incrementLoad();

        log.info("Relay '{}' selected and load incremented to {}/{} for parcel {}",
                selected.getName(), selected.getCurrentLoad(),
                selected.getMaxCapacity(), parcel.getTrackingNumber());

        return selected;
    }
}

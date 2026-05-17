package com.marketplace.delivery.application;

import com.marketplace.delivery.domain.Governorate;
import com.marketplace.delivery.domain.Hub;
import com.marketplace.delivery.exception.DeliveryNotFoundException;
import com.marketplace.delivery.infrastructure.HubRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Assigns the correct regional hub to a parcel based on its governorate.
 *
 * Each governorate has exactly one active hub. If no hub is found,
 * a DeliveryNotFoundException is thrown so the caller can handle gracefully.
 *
 * Future extensions:
 *   - Support multiple hubs per governorate (load balancing between hubs)
 *   - Add hub capacity management
 *   - Add fallback hub if primary hub is offline
 */
@Service
@RequiredArgsConstructor
public class HubAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(HubAssignmentService.class);

    private final HubRepository hubRepository;

    /**
     * Find and return the active hub for the given governorate.
     *
     * @param governorate Detected governorate of the delivery postal code.
     * @return The assigned Hub.
     * @throws DeliveryNotFoundException if no active hub exists for the governorate.
     */
    public Hub assignHub(Governorate governorate) {
        log.debug("Looking up active hub for governorate: {}", governorate);

        return hubRepository.findFirstByGovernorateAndActiveTrue(governorate)
                .orElseThrow(() -> {
                    log.error("No active hub available for governorate: {}", governorate);
                    return DeliveryNotFoundException.hub(governorate.getDisplayName());
                });
    }
}

package com.marketplace.delivery.application;

import com.marketplace.delivery.domain.Governorate;
import com.marketplace.delivery.exception.DeliveryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Detects the Tunisian Governorate from a 4-digit postal code prefix.
 *
 * This is a pure stateless service with no external dependencies.
 * Future: wrap with @Cacheable("postal-governorates") once Redis is added.
 */
@Service
public class GovernorateDetector {

    private static final Logger log = LoggerFactory.getLogger(GovernorateDetector.class);

    /**
     * Detect the governorate from a Tunisian 4-digit postal code.
     *
     * Mapping (first 2 digits):
     *   10–14 → Grand Tunis
     *   70–73 → Bizerte
     *   80–82 → Nabeul
     *
     * @param postalCode 4-digit Tunisian postal code
     * @return the detected Governorate
     * @throws DeliveryNotFoundException if the prefix is not recognized
     */
    public Governorate detect(String postalCode) {
        validatePostalCode(postalCode);

        try {
            Governorate gov = Governorate.fromPostalCode(postalCode);
            log.debug("Postal code {} → Governorate: {}", postalCode, gov.getDisplayName());
            return gov;
        } catch (IllegalArgumentException e) {
            log.warn("Unknown postal code prefix for: {}", postalCode);
            throw new DeliveryNotFoundException(
                    "Postal code '" + postalCode + "' is not covered by our delivery network.");
        }
    }

    private void validatePostalCode(String postalCode) {
        if (postalCode == null || postalCode.isBlank()) {
            throw new IllegalArgumentException("Postal code must not be blank");
        }
        if (!postalCode.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "Invalid Tunisian postal code format (expected 4 digits): " + postalCode);
        }
    }
}

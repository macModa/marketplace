package com.marketplace.delivery.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Tunisian governorates supported by the delivery system.
 * Each governorate is identified by its postal code prefixes (first 2 digits).
 *
 * Extension point: add more governorates as the network expands.
 */
public enum Governorate {

    GRAND_TUNIS("Grand Tunis", List.of("10", "11", "12", "13", "14")),
    BIZERTE("Bizerte", List.of("70", "71", "72", "73")),
    NABEUL("Nabeul", List.of("80", "81", "82"));

    private final String displayName;
    private final List<String> postalPrefixes;

    Governorate(String displayName, List<String> postalPrefixes) {
        this.displayName = displayName;
        this.postalPrefixes = postalPrefixes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getPostalPrefixes() {
        return postalPrefixes;
    }

    /**
     * Resolve governorate from a 4-digit Tunisian postal code.
     * Uses the first 2 digits as prefix.
     *
     * @param postalCode 4-digit postal code (e.g. "7010")
     * @return matching Governorate
     * @throws IllegalArgumentException if no governorate matches the prefix
     */
    public static Governorate fromPostalCode(String postalCode) {
        if (postalCode == null || postalCode.length() < 2) {
            throw new IllegalArgumentException("Postal code must be at least 2 digits: " + postalCode);
        }
        String prefix = postalCode.substring(0, 2);
        return Arrays.stream(values())
                .filter(g -> g.postalPrefixes.contains(prefix))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown postal code prefix '" + prefix + "' for postal code: " + postalCode));
    }
}

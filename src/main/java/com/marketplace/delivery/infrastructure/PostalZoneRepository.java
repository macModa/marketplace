package com.marketplace.delivery.infrastructure;

import com.marketplace.delivery.domain.PostalZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PostalZoneRepository extends JpaRepository<PostalZone, Long> {

    /**
     * Find postal zone by exact code.
     * Future: @Cacheable("postal-zones") once Redis is configured.
     */
    Optional<PostalZone> findByPostalCode(String postalCode);

    /**
     * Find existing zones for batch duplicate checking.
     */
    List<PostalZone> findByPostalCodeIn(Set<String> postalCodes);
}

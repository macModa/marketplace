package com.marketplace.delivery.infrastructure;

import com.marketplace.delivery.domain.DeliveryQrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryQrTokenRepository extends JpaRepository<DeliveryQrToken, Long> {

    Optional<DeliveryQrToken> findByTokenHashAndUsedFalse(String tokenHash);

    @Modifying
    @Query("UPDATE DeliveryQrToken t SET t.used = true WHERE t.trackingNumber = :trackingNumber AND t.used = false")
    void invalidateExistingTokens(String trackingNumber);
}

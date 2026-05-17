package com.marketplace.delivery.infrastructure;

import com.marketplace.delivery.domain.Governorate;
import com.marketplace.delivery.domain.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HubRepository extends JpaRepository<Hub, Long> {

    Optional<Hub> findFirstByGovernorateAndActiveTrue(Governorate governorate);

    List<Hub> findByGovernorate(Governorate governorate);

    List<Hub> findByActiveTrue();
}

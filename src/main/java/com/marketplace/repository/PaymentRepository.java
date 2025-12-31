package com.marketplace.repository;

import com.marketplace.entity.Payment;
import com.marketplace.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    
    Optional<Payment> findByReference(String reference);
    
    @Query("SELECT p FROM Payment p WHERE p.statut = :statut")
    java.util.List<Payment> findByStatut(@Param("statut") PaymentStatus statut);
}


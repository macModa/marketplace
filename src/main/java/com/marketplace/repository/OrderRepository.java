package com.marketplace.repository;

import com.marketplace.entity.Order;
import com.marketplace.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Page<Order> findByClientId(Long clientId, Pageable pageable);
    
    Page<Order> findByStatut(OrderStatus statut, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.client.id = :clientId AND o.statut = :statut")
    Page<Order> findByClientIdAndStatut(@Param("clientId") Long clientId,
                                        @Param("statut") OrderStatus statut,
                                        Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.dateCreation BETWEEN :startDate AND :endDate")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.statut = :statut")
    long countByStatut(@Param("statut") OrderStatus statut);
}


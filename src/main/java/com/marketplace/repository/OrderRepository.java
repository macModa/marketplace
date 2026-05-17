package com.marketplace.repository;

import com.marketplace.entity.Order;
import com.marketplace.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"client", "artisan"})
    Page<Order> findByClientId(Long clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "artisan"})
    Page<Order> findByStatut(OrderStatus statut, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "artisan"})
    @Query("SELECT o FROM Order o WHERE o.client.id = :clientId AND o.statut = :statut")
    Page<Order> findByClientIdAndStatut(@Param("clientId") Long clientId,
                                        @Param("statut") OrderStatus statut,
                                        Pageable pageable);

    @EntityGraph(attributePaths = {"client", "artisan"})
    Page<Order> findByArtisanId(Long artisanId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "artisan"})
    @Query("SELECT o FROM Order o WHERE o.artisan.id = :artisanId AND o.statut = :statut")
    Page<Order> findByArtisanIdAndStatut(@Param("artisanId") Long artisanId,
                                         @Param("statut") OrderStatus statut,
                                         Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"client"})
    Page<Order> findAll(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.dateCreation BETWEEN :startDate AND :endDate")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.statut = :statut")
    long countByStatut(@Param("statut") OrderStatus statut);

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.client
        LEFT JOIN FETCH o.artisan
        LEFT JOIN FETCH o.orderLines i
        LEFT JOIN FETCH i.product p
        LEFT JOIN FETCH p.artisan
        WHERE o.id = :id
    """)
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    Optional<Order> findByDeliveryToken(String deliveryToken);

    // Used by DeliveryQrService — matches the trackingNumber field on Order entity.
    // QR content format: "trackingNumber|deliveryToken" (see confirm_delivery_provider.dart)
    Optional<Order> findByTrackingNumber(String trackingNumber);
}

package com.marketplace.repository;

import com.marketplace.entity.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    List<OrderLine> findByOrderId(Long orderId);
    
    @Query("SELECT ol FROM OrderLine ol WHERE ol.product.id = :productId")
    List<OrderLine> findByProductId(@Param("productId") Long productId);
}


package com.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_lines", indexes = {
    @Index(name = "idx_orderline_order", columnList = "order_id"),
    @Index(name = "idx_orderline_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Integer quantite;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    // Business methods
    public BigDecimal getSubtotal() {
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }
    
    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être positive");
        }
        this.quantite = newQuantity;
        if (order != null) {
            order.recalculateTotal();
        }
    }
}


package com.marketplace.entity;

import com.marketplace.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_client", columnList = "client_id"),
    @Index(name = "idx_order_statut", columnList = "statut"),
    @Index(name = "idx_order_date", columnList = "dateCreation")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus statut = OrderStatus.PENDING;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
    
    private LocalDateTime dateModification;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderLine> orderLines = new ArrayList<>();
    
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;
    
    // Business methods
    public void addOrderLine(OrderLine orderLine) {
        orderLines.add(orderLine);
        orderLine.setOrder(this);
        recalculateTotal();
    }
    
    public void removeOrderLine(OrderLine orderLine) {
        orderLines.remove(orderLine);
        orderLine.setOrder(null);
        recalculateTotal();
    }
    
    public void recalculateTotal() {
        this.total = orderLines.stream()
            .map(line -> line.getPrixUnitaire().multiply(BigDecimal.valueOf(line.getQuantite())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.dateModification = LocalDateTime.now();
    }
    
    public boolean canBeCancelled() {
        return statut == OrderStatus.PENDING || statut == OrderStatus.CONFIRMED;
    }
    
    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("La commande ne peut pas être annulée dans son état actuel");
        }
        this.statut = OrderStatus.CANCELLED;
        this.dateModification = LocalDateTime.now();
    }
    
    public boolean isPaid() {
        return payment != null && payment.getStatut() == com.marketplace.enums.PaymentStatus.COMPLETED;
    }
}


package com.marketplace.entity;

import com.marketplace.enums.PaymentMethod;
import com.marketplace.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_order", columnList = "order_id"),
    @Index(name = "idx_payment_statut", columnList = "statut"),
    @Index(name = "idx_payment_reference", columnList = "reference")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod methode;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus statut = PaymentStatus.PENDING;
    
    @Column(unique = true, length = 100)
    private String reference;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
    
    private LocalDateTime dateModification;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    
    // Business methods
    public void complete() {
        if (this.statut == PaymentStatus.PENDING) {
            this.statut = PaymentStatus.COMPLETED;
            this.dateModification = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Le paiement ne peut être complété que s'il est en attente");
        }
    }
    
    public void fail() {
        this.statut = PaymentStatus.FAILED;
        this.dateModification = LocalDateTime.now();
    }
    
    public void refund() {
        if (this.statut == PaymentStatus.COMPLETED) {
            this.statut = PaymentStatus.REFUNDED;
            this.dateModification = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Seuls les paiements complétés peuvent être remboursés");
        }
    }
    
    public boolean isCompleted() {
        return statut == PaymentStatus.COMPLETED;
    }
}


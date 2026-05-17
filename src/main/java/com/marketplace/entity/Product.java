package com.marketplace.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_nom", columnList = "nom"),
    @Index(name = "idx_product_artisan", columnList = "artisan_id"),
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_stock", columnList = "stock")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String nom;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;
    
    @Column(nullable = false)
    private Integer stock = 0;
    
    @Column(name = "poids_kg", precision = 10, scale = 3)
    private BigDecimal poidsKg;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artisan_id", nullable = false)
    @JsonIgnore
    private Artisan artisan;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore
    private Category category;

    @Column(name = "image_url")
    private String imageUrl;


    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderLine> orderLines = new ArrayList<>();




    // Business methods
    public boolean isAvailable() {
        return stock > 0;
    }
    
    public boolean hasStock(int quantity) {
        return stock >= quantity;
    }
    
    public void decreaseStock(int quantity) {
        if (!hasStock(quantity)) {
            throw new IllegalStateException("Stock insuffisant pour le produit: " + nom);
        }
        this.stock -= quantity;
    }
    
    public void increaseStock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("La quantité doit être positive");
        }
        this.stock += quantity;
    }
    
    public BigDecimal calculateTotalPrice(int quantity) {
        return prix.multiply(BigDecimal.valueOf(quantity));
    }
}


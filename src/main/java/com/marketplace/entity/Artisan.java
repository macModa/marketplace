package com.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "artisans")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Artisan extends User {
    
    @Column(nullable = false, length = 200)
    private String nomBoutique;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Boolean verifie = false;
    
    @OneToMany(mappedBy = "artisan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();
    
    // Business methods
    public void addProduct(Product product) {
        products.add(product);
        product.setArtisan(this);
    }
    
    public void removeProduct(Product product) {
        products.remove(product);
        product.setArtisan(null);
    }
    
    public boolean hasProducts() {
        return products != null && !products.isEmpty();
    }
}


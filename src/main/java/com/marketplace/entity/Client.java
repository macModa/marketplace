package com.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Client extends User {
    
    @Column(length = 255)
    private String adresseLivraison;
    
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
    
    // Business methods
    public void addOrder(Order order) {
        orders.add(order);
        order.setClient(this);
    }
    
    public void removeOrder(Order order) {
        orders.remove(order);
        order.setClient(null);
    }
    
    public boolean hasOrders() {
        return orders != null && !orders.isEmpty();
    }
}


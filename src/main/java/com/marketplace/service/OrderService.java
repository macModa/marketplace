package com.marketplace.service;

import com.marketplace.dto.CreateOrderRequest;
import com.marketplace.dto.OrderDto;
import com.marketplace.dto.OrderLineDto;
import com.marketplace.entity.*;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.ClientRepository;
import com.marketplace.repository.OrderRepository;
import com.marketplace.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    
    @Transactional
    public Order createOrder(CreateOrderRequest request, Long clientId) {
        logger.info("Création d'une nouvelle commande pour le client: {}", clientId);
        
        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client non trouvé: " + clientId));
        
        Order order = new Order();
        order.setClient(client);
        order.setStatut(OrderStatus.PENDING);
        order.setTotal(BigDecimal.ZERO);
        
        List<OrderLine> orderLines = new ArrayList<>();
        
        for (OrderLineDto lineDto : request.getOrderLines()) {
            Product product = productRepository.findById(lineDto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + lineDto.getProductId()));
            
            if (!product.hasStock(lineDto.getQuantite())) {
                throw new IllegalStateException("Stock insuffisant pour le produit: " + product.getNom());
            }
            
            OrderLine orderLine = new OrderLine();
            orderLine.setProduct(product);
            orderLine.setQuantite(lineDto.getQuantite());
            orderLine.setPrixUnitaire(product.getPrix());
            orderLine.setOrder(order);
            
            orderLines.add(orderLine);
            
            // Decrease stock immediately
            product.decreaseStock(lineDto.getQuantite());
        }
        
        order.setOrderLines(orderLines);
        order.recalculateTotal();
        
        Order saved = orderRepository.save(order);
        logger.info("Commande créée avec succès: ID {}, Total: {}", saved.getId(), saved.getTotal());
        
        return saved;
    }
    
    @Transactional
    public Order cancelOrder(Long orderId, Long clientId) {
        logger.info("Annulation de la commande: {} par le client: {}", orderId, clientId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderId));
        
        if (!order.getClient().getId().equals(clientId)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à annuler cette commande");
        }
        
        if (!order.canBeCancelled()) {
            throw new IllegalStateException("La commande ne peut pas être annulée dans son état actuel");
        }
        
        // Restore stock
        for (OrderLine line : order.getOrderLines()) {
            line.getProduct().increaseStock(line.getQuantite());
        }
        
        order.cancel();
        
        Order saved = orderRepository.save(order);
        logger.info("Commande annulée avec succès: {}", orderId);
        
        return saved;
    }
    
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        logger.info("Mise à jour du statut de la commande: {} vers {}", orderId, newStatus);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderId));
        
        order.setStatut(newStatus);
        
        return orderRepository.save(order);
    }
    
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderId));
    }
    
    public Page<Order> getOrdersByClient(Long clientId, Pageable pageable) {
        return orderRepository.findByClientId(clientId, pageable);
    }
    
    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatut(status, pageable);
    }
    
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
}


package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.CreateOrderRequest;
import com.marketplace.dto.OrderDto;
import com.marketplace.entity.Order;
import com.marketplace.entity.User;
import com.marketplace.enums.OrderStatus;
import com.marketplace.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<Order>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Order order = orderService.createOrder(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Commande créée avec succès", order));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Order>> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
    
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<Page<Order>>> getMyOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderService.getOrdersByClient(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<Order>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<Order>> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Order order = orderService.cancelOrder(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Commande annulée avec succès", order));
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        Order order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Statut de la commande mis à jour", order));
    }
}


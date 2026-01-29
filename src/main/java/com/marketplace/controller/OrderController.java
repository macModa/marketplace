package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.CreateOrderRequest;
import com.marketplace.dto.OrderDto;
import com.marketplace.dto.OrderLineDto;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    // --- MAPPER HELPER ---
    private OrderDto mapToOrderDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setStatut(order.getStatut());
        dto.setTotal(order.getTotal());
        dto.setDateCreation(order.getDateCreation());
        dto.setDateModification(order.getDateModification());
        
        if (order.getClient() != null) {
            dto.setClientId(order.getClient().getId());
            dto.setClientNom(order.getClient().getNom());
        }
        
        if (order.getOrderLines() != null) {
            List<OrderLineDto> lines = order.getOrderLines().stream().map(line -> {
                OrderLineDto lineDto = new OrderLineDto();
                lineDto.setId(line.getId());
                lineDto.setQuantite(line.getQuantite());
                lineDto.setPrixUnitaire(line.getPrixUnitaire());
                // Calculate subtotal if not present
                if (line.getPrixUnitaire() != null && line.getQuantite() != null) {
                    lineDto.setSubtotal(line.getPrixUnitaire().multiply(BigDecimal.valueOf(line.getQuantite())));
                }
                
                if (line.getProduct() != null) {
                    lineDto.setProductId(line.getProduct().getId());
                    lineDto.setProductNom(line.getProduct().getNom());
                }
                return lineDto;
            }).collect(Collectors.toList());
            dto.setOrderLines(lines);
        }
        return dto;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Order order = orderService.createOrder(request, user.getId());
        
        // Return DTO
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Commande créée avec succès", mapToOrderDto(order)));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(mapToOrderDto(order)));
    }
    
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getMyOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderService.getOrdersByClient(user.getId(), pageable);
        
        return ResponseEntity.ok(ApiResponse.success(orders.map(this::mapToOrderDto)));
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders.map(this::mapToOrderDto)));
    }
    
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Order order = orderService.cancelOrder(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Commande annulée avec succès", mapToOrderDto(order)));
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        Order order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Statut de la commande mis à jour", mapToOrderDto(order)));
    }
}
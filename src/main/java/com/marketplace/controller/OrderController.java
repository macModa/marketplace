package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.ConfirmDeliveryRequest;
import com.marketplace.dto.CreateOrderRequest;
import com.marketplace.dto.OrderDto;
import com.marketplace.dto.OrderLineDto;
import com.marketplace.entity.Order;
import com.marketplace.entity.User;
import com.marketplace.enums.OrderStatus;
import com.marketplace.service.OrderService;
import com.marketplace.service.DeliveryService;
import com.marketplace.mapper.OrderMapper;
import com.marketplace.delivery.application.BonLivraisonService;
import com.marketplace.delivery.dto.BonLivraisonDTO;
import com.marketplace.repository.OrderRepository;
import com.marketplace.service.PdfService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
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

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final DeliveryService deliveryService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final BonLivraisonService bonLivraisonService;
    private final PdfService pdfService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrderDto orderDto = orderService.createOrder(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Commande créée avec succès", orderDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(
            @PathVariable Long id,
            Authentication authentication) {
        OrderDto orderDto = orderService.getOrderById(id);
        User user = (User) authentication.getPrincipal();

        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !orderDto.getClientId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Accès refusé"));
        }

        return ResponseEntity.ok(ApiResponse.success(orderDto));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasAnyRole('CLIENT', 'ARTISAN')")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getMyOrders(
            Authentication authentication,
            @RequestParam(required = false) OrderStatus statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);

        boolean isArtisan = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ARTISAN"));

        Page<OrderDto> orders;
        if (isArtisan) {
            orders = orderService.getOrdersByArtisan(user.getId(), statut, pageable);
        } else {
            orders = orderService.getOrdersByClient(user.getId(), statut, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/my-artisan-orders")
    @PreAuthorize("hasRole('ARTISAN')")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getMyArtisanOrders(
            Authentication authentication,
            @RequestParam(required = false) OrderStatus statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDto> orders = orderService.getOrdersByArtisan(user.getId(), statut, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDto> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrderDto orderDto = orderService.cancelOrder(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Commande annulée avec succès", orderDto));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        OrderDto orderDto = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour", orderDto));
    }

    // ✅ NOUVEAU — Artisan change le statut de sa commande
    // PENDING → CONFIRMED → SHIPPED
    @PutMapping("/{id}/artisan-status")
    @PreAuthorize("hasRole('ARTISAN')")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatusByArtisan(
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        // Vérifier que la commande appartient à cet artisan
        Order order = orderService.findOrderById(id);
        if (order.getArtisan() == null || !order.getArtisan().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Vous n'êtes pas autorisé à modifier cette commande"));
        }

        // Artisan ne peut que CONFIRMER ou EXPÉDIER (pas DELIVERED ni CANCELLED)
        if (status != OrderStatus.CONFIRMED && status != OrderStatus.SHIPPED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Statut non autorisé pour l'artisan. Utilisez CONFIRMED ou SHIPPED."));
        }

        OrderDto orderDto = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour vers " + status, orderDto));
    }

    // ✅ NOUVEAU — endpoint compatible avec Flutter /api/v1/delivery/qr/confirm
    // Body: { "token": "...", "trackingNumber": "..." }
    @PostMapping("/confirm-delivery-qr")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<OrderDto>> confirmDeliveryByQr(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            String token = body.get("token");

            if (token == null || token.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("token requis"));
            }

            // Trouver la commande par deliveryToken
            Order order = orderRepository.findByDeliveryToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide ou commande non trouvée"));

            Order updated = deliveryService.validateDelivery(order.getId(), token, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Livraison confirmée avec succès",
                orderMapper.toDto(updated)));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur confirmation livraison QR", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Erreur serveur"));
        }
    }

    @GetMapping("/{id}/scan")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<?>> getScanData(@PathVariable Long id, Authentication auth) {
        Order order = orderService.findOrderById(id);
        String userEmail = auth.getName();

        if (!order.getClient().getEmail().equals(userEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Accès refusé"));
        }

        var data = Map.of(
            "orderId", order.getId(),
            "deliveryToken", order.getDeliveryToken() != null ? order.getDeliveryToken() : "",
            "statut", order.getStatut() != null ? order.getStatut().name() : ""
        );

        return ResponseEntity.ok(ApiResponse.success("Données de scan récupérées", data));
    }

    @PostMapping("/{id}/validate-delivery")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<OrderDto>> validateDelivery(
            @PathVariable Long id,
            @RequestParam String token,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Order updatedOrder = deliveryService.validateDelivery(id, token, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Livraison confirmée",
                orderMapper.toDto(updatedOrder)));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur validation livraison", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Erreur serveur"));
        }
    }

    @PostMapping("/scan")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<OrderDto>> scanDelivery(
            @Valid @RequestBody ConfirmDeliveryRequest request) {
        try {
            Order updatedOrder = deliveryService.validateDelivery(
                request.orderId(), request.token(), null);
            return ResponseEntity.ok(ApiResponse.success("Livraison validée",
                orderMapper.toDto(updatedOrder)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

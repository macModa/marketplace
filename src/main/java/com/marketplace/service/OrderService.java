package com.marketplace.service;

import com.marketplace.dto.CreateOrderRequest;
import com.marketplace.dto.OrderDto;
import com.marketplace.dto.OrderLineDto;
import com.marketplace.entity.*;
import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.PaymentMethod;
import com.marketplace.enums.PaymentStatus;
import com.marketplace.repository.ClientRepository;
import com.marketplace.repository.OrderRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.mapper.OrderMapper;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, Long clientId) {
        logger.info("Création d'une nouvelle commande pour le client: {}", clientId);

        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client non trouvé: " + clientId));

        Order order = new Order();
        order.setClient(client);
        order.setStatut(OrderStatus.PENDING);
        order.setTotal(BigDecimal.ZERO);

        if (request.getPaymentMethod() != null) {
            try {
                PaymentMethod pm = PaymentMethod.valueOf(request.getPaymentMethod());
                order.setPaymentMethod(pm);
                if (pm == PaymentMethod.CASH_ON_DELIVERY) {
                    order.setPaymentStatus(PaymentStatus.PENDING);
                } else if (pm == PaymentMethod.ONLINE) {
                    order.setPaymentStatus(PaymentStatus.PAID);
                    order.setPaidAt(java.time.LocalDateTime.now());
                } else {
                    order.setPaymentStatus(PaymentStatus.PENDING);
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid payment method: {}", request.getPaymentMethod());
                order.setPaymentMethod(PaymentMethod.ONLINE);
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setPaidAt(java.time.LocalDateTime.now());
            }
        } else {
            order.setPaymentMethod(PaymentMethod.ONLINE);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(java.time.LocalDateTime.now());
        }

        List<OrderLine> orderLines = new ArrayList<>();
        Artisan artisan = null;

        for (OrderLineDto lineDto : request.getOrderLines()) {
            Product product = productRepository.findById(lineDto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + lineDto.getProductId()));

            if (!product.hasStock(lineDto.getQuantite())) {
                throw new IllegalStateException("Stock insuffisant pour le produit: " + product.getNom());
            }

            if (artisan == null && product.getArtisan() != null) {
                artisan = product.getArtisan();
            }

            OrderLine orderLine = new OrderLine();
            orderLine.setProduct(product);
            orderLine.setQuantite(lineDto.getQuantite());
            orderLine.setPrixUnitaire(product.getPrix());
            orderLine.setOrder(order);

            orderLines.add(orderLine);
            product.decreaseStock(lineDto.getQuantite());
        }

        if (artisan != null) {
            order.setArtisan(artisan);
            logger.info("Artisan assigné à la commande: {}", artisan.getId());
        } else {
            logger.warn("Aucun artisan trouvé pour la commande du client {}", clientId);
        }

        order.setOrderLines(orderLines);
        order.recalculateTotal();

        Order saved = orderRepository.save(order);
        logger.info("Commande créée avec succès: ID {}, Total: {}", saved.getId(), saved.getTotal());

        return orderMapper.toDto(saved);
    }

    @Transactional
    public OrderDto cancelOrder(Long orderId, Long clientId) {
        logger.info("Annulation de la commande: {} par le client: {}", orderId, clientId);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderId));

        if (!order.getClient().getId().equals(clientId)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à annuler cette commande");
        }

        if (!order.canBeCancelled()) {
            throw new IllegalStateException("La commande ne peut pas être annulée dans son état actuel");
        }

        for (OrderLine line : order.getOrderLines()) {
            line.getProduct().increaseStock(line.getQuantite());
        }

        order.cancel();

        Order saved = orderRepository.save(order);
        logger.info("Commande annulée avec succès: {}", orderId);

        return orderMapper.toDto(saved);
    }

    @Transactional
    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        logger.info("Mise à jour du statut de la commande: {} vers {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderId));

        // ✅ FIX: Générer tracking number + delivery token lors du passage en SHIPPED
        if (newStatus == OrderStatus.SHIPPED) {
            // Générer tracking number si absent ou valeur par défaut
            if (order.getTrackingNumber() == null
                    || order.getTrackingNumber().isBlank()
                    || order.getTrackingNumber().equals("EN_ATTENTE")) {
                String trackingNumber = "MRK-" + orderId + "-" + System.currentTimeMillis();
                order.setTrackingNumber(trackingNumber);
                logger.info("Tracking number généré pour commande {}: {}", orderId, trackingNumber);
            }

            // Générer delivery token si absent
            if (order.getDeliveryToken() == null || order.getDeliveryToken().isBlank()) {
                String deliveryToken = UUID.randomUUID().toString();
                order.setDeliveryToken(deliveryToken);
                logger.info("Delivery token généré pour commande {}: {}", orderId, deliveryToken);
            }

            order.setDateModification(java.time.LocalDateTime.now());
        }

        order.setStatut(newStatus);

        Order saved = orderRepository.save(order);
        logger.info("Statut commande {} mis à jour: {}", orderId, newStatus);
        return orderMapper.toDto(saved);
    }

    @Transactional
    public void completeCodPayment(Long orderId) {
        logger.info("Tentative de complétion de paiement COD pour la commande: {}", orderId);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderId));

        if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                && order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(java.time.LocalDateTime.now());
            orderRepository.save(order);
            logger.info("Paiement COD complété avec succès pour la commande: {}", orderId);
        } else {
            logger.warn("Impossible de compléter le paiement COD : méthode={} statut={}",
                order.getPaymentMethod(), order.getPaymentStatus());
        }
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderId));
        return orderMapper.toDto(order);
    }

    @Transactional(readOnly = true)
    public Order findOrderById(Long id) {
        return orderRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + id));
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByClient(Long clientId, OrderStatus status, Pageable pageable) {
        if (status != null) {
            return orderRepository.findByClientIdAndStatut(clientId, status, pageable)
                    .map(orderMapper::toDto);
        }
        return orderRepository.findByClientId(clientId, pageable)
                .map(orderMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByArtisan(Long artisanId, OrderStatus status, Pageable pageable) {
        if (status != null) {
            return orderRepository.findByArtisanIdAndStatut(artisanId, status, pageable)
                    .map(orderMapper::toDto);
        }
        return orderRepository.findByArtisanId(artisanId, pageable)
                .map(orderMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatut(status, pageable)
                .map(orderMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toDto);
    }

    @Transactional
    public OrderDto shipOrder(Long orderId, Long artisanId) {
        logger.info("Expédition de la commande: {} par l'artisan: {}", orderId, artisanId);

        Order order = orderRepository.findByIdWithDetails(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderId));

        if (order.getArtisan() == null || !order.getArtisan().getId().equals(artisanId)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à expédier cette commande");
        }

        if (order.getStatut() != OrderStatus.CONFIRMED && order.getStatut() != OrderStatus.PENDING) {
            throw new IllegalStateException("La commande doit être PENDING ou CONFIRMED pour être expédiée");
        }

        // Générer tracking number
        String trackingNumber = "MRK-" + orderId + "-" + System.currentTimeMillis();
        order.setTrackingNumber(trackingNumber);

        // Générer delivery token
        String deliveryToken = UUID.randomUUID().toString();
        order.setDeliveryToken(deliveryToken);

        order.setStatut(OrderStatus.SHIPPED);
        order.setDateModification(java.time.LocalDateTime.now());

        Order saved = orderRepository.save(order);
        logger.info("Commande {} expédiée avec tracking: {} et token: {}", orderId, trackingNumber, deliveryToken);
        return orderMapper.toDto(saved);
    }
}

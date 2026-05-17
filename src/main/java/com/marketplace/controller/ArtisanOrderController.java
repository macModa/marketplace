package com.marketplace.controller;

import com.marketplace.entity.Order;
import com.marketplace.repository.OrderRepository;
import com.marketplace.delivery.application.BonLivraisonService;
import com.marketplace.delivery.dto.BonLivraisonDTO;
import com.marketplace.service.DeliveryService;
import jakarta.transaction.Transactional;                          // ← AJOUT
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/artisan/orders")
@RequiredArgsConstructor
public class ArtisanOrderController {

    private static final Logger logger = LoggerFactory.getLogger(ArtisanOrderController.class);

    private final OrderRepository orderRepository;
    private final DeliveryService deliveryService;
    private final BonLivraisonService bonLivraisonService;

    @GetMapping("/{id}/delivery-note")
    @PreAuthorize("hasRole('ARTISAN') or hasRole('ADMIN')")
    @Transactional                                                 // ← AJOUT
    public ResponseEntity<byte[]> generateDeliveryNote(@PathVariable Long id, Authentication auth) {
        logger.info("Demande de Bon de Livraison (PDF) pour la commande {} par l'artisan {}", id, auth.getName());
        try {
            Order order = orderRepository.findByIdWithDetails(id) // ← CHANGEMENT
                    .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée"));

            // Debug temporaire — à supprimer après confirmation
            logger.info("Artisan en base = {}, Auth email = {}",
                order.getArtisan() != null ? order.getArtisan().getEmail() : "NULL",
                auth.getName());

            if (order.getArtisan() == null || !order.getArtisan().getEmail().equalsIgnoreCase(auth.getName())) {
                logger.warn("Tentative d'accès non autorisée au PDF de la commande {} par {}", id, auth.getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String token = deliveryService.getOrGenerateDeliveryToken(order);
            BonLivraisonDTO dto = bonLivraisonService.getBonLivraison(id);
            byte[] pdfBytes = deliveryService.generateDeliveryPdf(dto, token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("bon_livraison_" + id + ".pdf")
                    .build());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.warn("Commande non trouvée : {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Erreur critique lors de la génération du PDF pour la commande {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
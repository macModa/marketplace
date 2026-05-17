package com.marketplace.delivery.infrastructure.web;

import com.marketplace.delivery.application.DeliveryNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryNoteController {

    private final DeliveryNoteService deliveryNoteService;

    /**
     * GET /api/delivery/bon-livraison/{orderId}
     * Retourne le bon de livraison en PDF pour une commande donnée.
     * Accessible uniquement par l'artisan propriétaire ou un admin.
     */
    @GetMapping("/bon-livraison/{orderId}")
    @PreAuthorize("hasAnyRole('ARTISAN', 'ADMIN')")
    public ResponseEntity<byte[]> getDeliveryNote(@PathVariable Long orderId) {
        byte[] pdfBytes = deliveryNoteService.generateDeliveryNotePdf(orderId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"bon_livraison_" + orderId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }
}

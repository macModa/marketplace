package com.marketplace.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for the CP1-style Bon de Livraison screen.
 *
 * Assembles data from three bounded contexts:
 *   - Order domain   → products, totals, dates
 *   - Delivery domain → tracking number, postal code, parcel weight
 *   - User domain    → artisan (expediteur) + client (destinataire)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonLivraisonDTO {

    private Long      orderId;

    /** Parcel tracking number (format DLV-YYYYMMDD-XXXXXXXX). */
    private String    numeroSuivi;

    private ExpéditeurDTO   expéditeur;
    private DestinataireDTO destinataire;
    private List<ProduitDTO> produits;

    /** Sum of (poids_kg × quantité) for all order lines. */
    private BigDecimal poidsTotal;

    /** Amount the delivery agent must collect (= order total for COD). */
    private BigDecimal montantCOD;

    /** Human-readable French amount string (e.g. "Cent quarante-cinq dinars"). */
    private String    montantEnLettres;

    /** ISO-8601 order creation timestamp. */
    private String    dateCreation;

    /** OrderStatus enum name. */
    private String    statut;

    /** Always "CONTRE_REMBOURSEMENT" for now. */
    private String    modeReglement;

    // ─── Nested DTOs ─────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpéditeurDTO {
        /** Full name of the artisan (User.nom). */
        private String nom;

        /** Store / boutique name (Artisan.nomBoutique). */
        private String nomBoutique;

        /** City / address (User.ville). */
        private String ville;

        /** Phone number (User.telephone). */
        private String telephone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DestinataireDTO {
        /** Full name of the client (User.nom). */
        private String nom;

        /** Delivery address stored on the client profile. */
        private String adresseLivraison;

        /** 4-digit Tunisian postal code from the parcel record. */
        private String codePostal;

        /** Phone number (User.telephone). */
        private String telephone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProduitDTO {
        private String     nom;
        private Integer    quantite;
        private BigDecimal prixUnitaire;

        /** Unit weight in kg (Product.poidsKg, default 0.500). */
        private BigDecimal poidsUnitaire;

        /** prixUnitaire × quantite */
        private BigDecimal sousTotal;
    }
}

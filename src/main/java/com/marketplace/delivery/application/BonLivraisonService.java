package com.marketplace.delivery.application;

import com.marketplace.delivery.dto.BonLivraisonDTO;
import com.marketplace.delivery.exception.DeliveryNotFoundException;
import com.marketplace.entity.Artisan;
import com.marketplace.entity.Client;
import com.marketplace.entity.Order;
import com.marketplace.entity.OrderLine;
import com.marketplace.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BonLivraisonService {

    private final OrderRepository orderRepository;

    public BonLivraisonDTO getBonLivraison(Long orderId) {

        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new DeliveryNotFoundException(
                        "Commande introuvable : " + orderId));

        if (order.getOrderLines().isEmpty()) {
            throw new IllegalStateException(
                    "La commande " + orderId + " ne contient aucune ligne.");
        }

        Artisan artisan = order.getArtisan();
        if (artisan == null) {
            throw new IllegalStateException(
                "La commande " + orderId + " n'a pas d'artisan assigné.");
        }
        Client client = order.getClient();

        // ✅ Lire trackingNumber directement depuis Order
        String trackingNumber = order.getTrackingNumber() != null
                ? order.getTrackingNumber()
                : "EN_ATTENTE";

        // Client n'a pas de codePostal — valeur par défaut
        String postalCode = "—";

        List<BonLivraisonDTO.ProduitDTO> produits = order.getOrderLines().stream()
                .map(this::toProduitDTO)
                .collect(Collectors.toList());

        BigDecimal poidsTotal = produits.stream()
                .map(p -> p.getPoidsUnitaire()
                            .multiply(BigDecimal.valueOf(p.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal montantCOD = order.getTotal();

        return BonLivraisonDTO.builder()
                .orderId(orderId)
                .numeroSuivi(trackingNumber)
                .expéditeur(toExpéditeurDTO(artisan))
                .destinataire(toDestinataireDTO(client, postalCode))
                .produits(produits)
                .poidsTotal(poidsTotal)
                .montantCOD(montantCOD)
                .montantEnLettres(numberToWordsFr(montantCOD))
                .dateCreation(order.getDateCreation().toString())
                .statut(order.getStatut().name())
                .modeReglement("CONTRE_REMBOURSEMENT")
                .build();
    }

    private BonLivraisonDTO.ExpéditeurDTO toExpéditeurDTO(Artisan artisan) {
        return BonLivraisonDTO.ExpéditeurDTO.builder()
                .nom(artisan.getNom())
                .nomBoutique(artisan.getNomBoutique())
                .ville(artisan.getVille() != null ? artisan.getVille() : "—")
                .telephone(artisan.getTelephone() != null ? artisan.getTelephone() : "—")
                .build();
    }

    private BonLivraisonDTO.DestinataireDTO toDestinataireDTO(Client client, String postalCode) {
        return BonLivraisonDTO.DestinataireDTO.builder()
                .nom(client.getNom())
                .adresseLivraison(client.getAdresseLivraison() != null
                        ? client.getAdresseLivraison() : "—")
                .codePostal(postalCode)
                .telephone(client.getTelephone() != null ? client.getTelephone() : "—")
                .build();
    }

    private BonLivraisonDTO.ProduitDTO toProduitDTO(OrderLine line) {
        BigDecimal poids = line.getProduct().getPoidsKg() != null
                ? line.getProduct().getPoidsKg()
                : BigDecimal.valueOf(0.500);

        return BonLivraisonDTO.ProduitDTO.builder()
                .nom(line.getProduct().getNom())
                .quantite(line.getQuantite())
                .prixUnitaire(line.getPrixUnitaire())
                .poidsUnitaire(poids)
                .sousTotal(line.getSubtotal())
                .build();
    }

    public String numberToWordsFr(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Zéro dinar";
        }
        int dinars = amount.intValue();
        int millimes = amount
                .subtract(BigDecimal.valueOf(dinars))
                .multiply(BigDecimal.valueOf(1000))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        StringBuilder sb = new StringBuilder();
        sb.append(intToWordsFr(dinars)).append(dinars > 1 ? " dinars" : " dinar");
        if (millimes > 0) {
            sb.append(" et ").append(intToWordsFr(millimes))
              .append(millimes > 1 ? " millimes" : " millime");
        }
        String result = sb.toString();
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    private String intToWordsFr(int n) {
        if (n == 0) return "zéro";
        if (n < 0)  return "moins " + intToWordsFr(-n);
        final String[] ONES = {
            "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit",
            "neuf", "dix", "onze", "douze", "treize", "quatorze", "quinze",
            "seize", "dix-sept", "dix-huit", "dix-neuf"
        };
        final String[] TENS = {
            "", "", "vingt", "trente", "quarante", "cinquante",
            "soixante", "soixante", "quatre-vingt", "quatre-vingt"
        };
        if (n < 20) return ONES[n];
        if (n < 70) {
            int ten = n / 10, unit = n % 10;
            if (unit == 0) return TENS[ten] + (ten == 8 ? "s" : "");
            if (unit == 1 && ten < 8) return TENS[ten] + " et un";
            return TENS[ten] + "-" + ONES[unit];
        }
        if (n < 80) return "soixante-" + ONES[n - 60];
        if (n < 90) { int unit = n - 80; return unit == 0 ? "quatre-vingts" : "quatre-vingt-" + ONES[unit]; }
        if (n < 100) return "quatre-vingt-" + ONES[n - 80];
        if (n < 200) { int rem = n - 100; return rem == 0 ? "cent" : "cent " + intToWordsFr(rem); }
        if (n < 1000) { int h = n / 100, rem = n % 100; String hs = ONES[h] + " cent" + (rem == 0 ? "s" : ""); return rem == 0 ? hs : hs + " " + intToWordsFr(rem); }
        if (n < 2000) { int rem = n - 1000; return rem == 0 ? "mille" : "mille " + intToWordsFr(rem); }
        if (n < 1_000_000) { int t = n / 1000, rem = n % 1000; String ts = intToWordsFr(t) + " mille"; return rem == 0 ? ts : ts + " " + intToWordsFr(rem); }
        return String.valueOf(n);
    }
}

package com.marketplace.delivery.api;

import com.marketplace.delivery.application.BonLivraisonService;
import com.marketplace.delivery.dto.BonLivraisonDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

@RestController
@RequestMapping("/api/v1/delivery/bon-livraison")
@RequiredArgsConstructor
public class BonLivraisonController {

    private static final Logger logger = LoggerFactory.getLogger(BonLivraisonController.class);
    private final BonLivraisonService bonLivraisonService;

    // ── GET JSON ──────────────────────────────────────────────────────────────
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ARTISAN')")
    public ResponseEntity<BonLivraisonDTO> getBonLivraison(@PathVariable Long orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            logger.info("User: {} | Roles: {}", auth.getName(),
                auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList());
        }
        BonLivraisonDTO dto = bonLivraisonService.getBonLivraison(orderId);
        return ResponseEntity.ok(dto);
    }

    // ── GET PDF ───────────────────────────────────────────────────────────────
    @GetMapping("/{orderId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'ARTISAN')")
    public ResponseEntity<byte[]> getBonLivraisonPdf(@PathVariable Long orderId) {
        BonLivraisonDTO dto = bonLivraisonService.getBonLivraison(orderId);
        try {
            byte[] pdf = generatePdf(dto);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"bon_livraison_" + orderId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            logger.error("Erreur génération PDF commande {}", orderId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── PDF GENERATION ────────────────────────────────────────────────────────
    private byte[] generatePdf(BonLivraisonDTO dto) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font fontTitle  = new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE);
        Font fontBold   = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font fontNormal = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font fontSmall  = new Font(Font.HELVETICA,  8, Font.NORMAL, Color.GRAY);

        // ── Header ────────────────────────────────────────────────────────────
        // 3 colonnes : titre | infos commande | QR code
        PdfPTable header = new PdfPTable(3);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3, 2, 1.2f});

        // Colonne 1 : titre
        PdfPCell titleCell = new PdfPCell(new Phrase("BON DE LIVRAISON", fontTitle));
        titleCell.setBackgroundColor(new Color(139, 90, 43));
        titleCell.setPadding(12);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.addCell(titleCell);

        // Colonne 2 : infos commande
        PdfPCell orderCell = new PdfPCell();
        orderCell.addElement(new Phrase("Commande #" + dto.getOrderId(), fontBold));
        orderCell.addElement(new Phrase("Suivi: " + dto.getNumeroSuivi(), fontNormal));
        orderCell.addElement(new Phrase("Date: " + dto.getDateCreation().substring(0, 10), fontNormal));
        orderCell.addElement(new Phrase("Statut: " + dto.getStatut(), fontNormal));
        orderCell.setPadding(10);
        orderCell.setBorder(Rectangle.NO_BORDER);
        orderCell.setBackgroundColor(new Color(245, 245, 245));
        header.addCell(orderCell);

        // Colonne 3 : QR code (tracking number)
        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setBackgroundColor(new Color(245, 245, 245));
        qrCell.setPadding(6);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String trackingNumber = dto.getNumeroSuivi();
        if (trackingNumber != null && !trackingNumber.equals("EN_ATTENTE")) {
            try {
                byte[] qrBytes = generateQrCodeBytes(trackingNumber, 120, 120);
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.scaleToFit(90, 90);
                qrCell.addElement(qrImage);
                qrCell.addElement(new Phrase(trackingNumber,
                    new Font(Font.HELVETICA, 6, Font.NORMAL, Color.DARK_GRAY)));
            } catch (Exception e) {
                logger.warn("QR code generation failed: {}", e.getMessage());
                qrCell.addElement(new Phrase("N° Suivi:\n" + trackingNumber, fontSmall));
            }
        } else {
            qrCell.addElement(new Phrase("En attente\nd'expédition", fontSmall));
        }
        header.addCell(qrCell);

        doc.add(header);
        doc.add(Chunk.NEWLINE);

        // ── Expéditeur / Destinataire ─────────────────────────────────────────
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        parties.setSpacingBefore(8);

        PdfPCell expCell = new PdfPCell();
        expCell.addElement(new Phrase("EXPÉDITEUR", fontBold));
        expCell.addElement(new Phrase(dto.getExpéditeur().getNomBoutique(), fontNormal));
        expCell.addElement(new Phrase(dto.getExpéditeur().getNom(), fontNormal));
        expCell.addElement(new Phrase("Ville: " + dto.getExpéditeur().getVille(), fontNormal));
        expCell.addElement(new Phrase("Tél: " + dto.getExpéditeur().getTelephone(), fontNormal));
        expCell.setPadding(10);
        expCell.setBackgroundColor(new Color(252, 248, 244));
        parties.addCell(expCell);

        PdfPCell destCell = new PdfPCell();
        destCell.addElement(new Phrase("DESTINATAIRE", fontBold));
        destCell.addElement(new Phrase(dto.getDestinataire().getNom(), fontNormal));
        destCell.addElement(new Phrase(dto.getDestinataire().getAdresseLivraison(), fontNormal));
        destCell.addElement(new Phrase("CP: " + dto.getDestinataire().getCodePostal(), fontNormal));
        destCell.addElement(new Phrase("Tél: " + dto.getDestinataire().getTelephone(), fontNormal));
        destCell.setPadding(10);
        destCell.setBackgroundColor(new Color(244, 248, 252));
        parties.addCell(destCell);
        doc.add(parties);
        doc.add(Chunk.NEWLINE);

        // ── Tableau produits ──────────────────────────────────────────────────
        PdfPTable produits = new PdfPTable(5);
        produits.setWidthPercentage(100);
        produits.setWidths(new float[]{4, 1, 2, 1.5f, 2});
        produits.setSpacingBefore(8);

        for (String h : new String[]{"Produit", "Qté", "Prix Unitaire", "Poids", "Sous-total"}) {
            PdfPCell c = new PdfPCell(
                new Phrase(h, new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            c.setBackgroundColor(new Color(139, 90, 43));
            c.setPadding(6);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            produits.addCell(c);
        }

        boolean alt = false;
        for (BonLivraisonDTO.ProduitDTO p : dto.getProduits()) {
            Color bg = alt ? new Color(249, 245, 241) : Color.WHITE;
            addCell(produits, p.getNom(),                          fontNormal, bg, Element.ALIGN_LEFT);
            addCell(produits, String.valueOf(p.getQuantite()),     fontNormal, bg, Element.ALIGN_CENTER);
            addCell(produits, p.getPrixUnitaire() + " TND",       fontNormal, bg, Element.ALIGN_RIGHT);
            addCell(produits, p.getPoidsUnitaire() + " kg",       fontNormal, bg, Element.ALIGN_CENTER);
            addCell(produits, p.getSousTotal() + " TND",          fontBold,   bg, Element.ALIGN_RIGHT);
            alt = !alt;
        }
        doc.add(produits);
        doc.add(Chunk.NEWLINE);

        // ── Totaux ────────────────────────────────────────────────────────────
        PdfPTable totaux = new PdfPTable(2);
        totaux.setWidthPercentage(45);
        totaux.setHorizontalAlignment(Element.ALIGN_RIGHT);
        addTotalRow(totaux, "Poids total :",  dto.getPoidsTotal() + " kg",  fontNormal, fontBold);
        addTotalRow(totaux, "Montant COD :",  dto.getMontantCOD() + " TND", fontNormal, fontBold);
        addTotalRow(totaux, "Mode :",         dto.getModeReglement(),        fontNormal, fontNormal);
        doc.add(totaux);
        doc.add(Chunk.NEWLINE);

        // ── Montant en lettres ────────────────────────────────────────────────
        PdfPTable lettres = new PdfPTable(1);
        lettres.setWidthPercentage(100);
        PdfPCell lc = new PdfPCell(
            new Phrase("Arrêté à : " + dto.getMontantEnLettres(), fontNormal));
        lc.setPadding(8);
        lc.setBackgroundColor(new Color(245, 245, 245));
        lettres.addCell(lc);
        doc.add(lettres);
        doc.add(Chunk.NEWLINE);

        // ── Signatures ────────────────────────────────────────────────────────
        PdfPTable signs = new PdfPTable(2);
        signs.setWidthPercentage(100);
        signs.setSpacingBefore(20);
        PdfPCell s1 = new PdfPCell(
            new Phrase("Signature Expéditeur\n\n\n_________________", fontNormal));
        s1.setPadding(10); s1.setMinimumHeight(60);
        signs.addCell(s1);
        PdfPCell s2 = new PdfPCell(
            new Phrase("Signature Destinataire\n\n\n_________________", fontNormal));
        s2.setPadding(10);
        signs.addCell(s2);
        doc.add(signs);

        // ── Footer ────────────────────────────────────────────────────────────
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph(
            "Document généré automatiquement par Marchi Marketplace", fontSmall);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return baos.toByteArray();
    }

    // ── QR Code Generator ─────────────────────────────────────────────────────
    private byte[] generateQrCodeBytes(String content, int width, int height) throws Exception {
        BitMatrix bitMatrix = new MultiFormatWriter()
                .encode(content, BarcodeFormat.QR_CODE, width, height);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", qrBaos);
        return qrBaos.toByteArray();
    }

    private void addCell(PdfPTable t, String text, Font font, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "-", font));
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void addTotalRow(PdfPTable t, String label, String value,
                              Font lf, Font vf) {
        PdfPCell l = new PdfPCell(new Phrase(label, lf));
        l.setBorder(Rectangle.BOTTOM); l.setPadding(5);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value != null ? value : "-", vf));
        v.setBorder(Rectangle.BOTTOM); v.setPadding(5);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(v);
    }
}

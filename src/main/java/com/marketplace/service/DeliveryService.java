package com.marketplace.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.marketplace.entity.Order;
import com.marketplace.entity.OrderLine;
import com.marketplace.enums.OrderStatus;
import com.marketplace.delivery.dto.BonLivraisonDTO;
import com.marketplace.repository.OrderRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final OrderRepository orderRepository;

    /**
     * Génère ou récupère un token de livraison sécurisé (expire dans 7 jours).
     */
    @Transactional
    public String getOrGenerateDeliveryToken(Order order) {
        if (order.getDeliveryToken() == null || order.getDeliveryTokenExpiry() == null 
                || order.getDeliveryTokenExpiry().isBefore(LocalDateTime.now())) {
            
            order.setDeliveryToken(UUID.randomUUID().toString());
            order.setDeliveryTokenExpiry(LocalDateTime.now().plusDays(7));
            orderRepository.save(order);
        }
        return order.getDeliveryToken();
    }

    /**
     * Génère le PDF du Bon de Livraison (CP1-style) avec le QR code.
     * Utilise les données assemblées dans BonLivraisonDTO.
     */
    public byte[] generateDeliveryPdf(BonLivraisonDTO dto, String token) throws Exception {
        // URL sécurisée pour le QR code
        // Note: Le frontend complétera l'URL avec le domaine si nécessaire
        String qrContent = String.format("/api/orders/%d/validate-delivery?token=%s", dto.getOrderId(), token); 

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(document, baos);

        document.open();

        // Styles
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.BLACK);
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Font boldNormalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

        // Header Table (Title and Order ID)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 1});
        
        PdfPCell titleCell = new PdfPCell(new Phrase("BON DE LIVRAISON", titleFont));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(titleCell);
        
        PdfPCell orderIdCell = new PdfPCell();
        orderIdCell.setBorder(Rectangle.NO_BORDER);
        orderIdCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        orderIdCell.addElement(new Paragraph("Commande #" + dto.getOrderId(), subTitleFont));
        orderIdCell.addElement(new Paragraph("Date: " + dto.getDateCreation(), smallFont));
        headerTable.addCell(orderIdCell);
        
        document.add(headerTable);
        document.add(new Paragraph(" "));
        document.add(new Chunk(new LineSeparator(1f, 100, Color.LIGHT_GRAY, Element.ALIGN_CENTER, -2)));
        document.add(new Paragraph(" "));

        // Info Section (Expéditeur vs Destinataire) - CP1 Style boxes
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10f);
        infoTable.setSpacingAfter(10f);
        
        // Expéditeur
        PdfPCell expCell = new PdfPCell();
        expCell.setPadding(10f);
        expCell.setBackgroundColor(new Color(245, 245, 245));
        expCell.setBorderColor(Color.LIGHT_GRAY);
        expCell.addElement(new Paragraph("EXPÉDITEUR", smallFont));
        expCell.addElement(new Paragraph(dto.getExpéditeur().getNomBoutique(), boldNormalFont));
        expCell.addElement(new Paragraph(dto.getExpéditeur().getNom(), normalFont));
        expCell.addElement(new Paragraph(dto.getExpéditeur().getVille(), normalFont));
        expCell.addElement(new Paragraph("Tél: " + dto.getExpéditeur().getTelephone(), normalFont));
        infoTable.addCell(expCell);
        
        // Destinataire
        PdfPCell destCell = new PdfPCell();
        destCell.setPadding(10f);
        destCell.setBorderColor(Color.LIGHT_GRAY);
        destCell.addElement(new Paragraph("DESTINATAIRE", smallFont));
        destCell.addElement(new Paragraph(dto.getDestinataire().getNom(), boldNormalFont));
        destCell.addElement(new Paragraph(dto.getDestinataire().getAdresseLivraison(), normalFont));
        destCell.addElement(new Paragraph("Code Postal: " + dto.getDestinataire().getCodePostal(), normalFont));
        destCell.addElement(new Paragraph("Tél: " + dto.getDestinataire().getTelephone(), normalFont));
        infoTable.addCell(destCell);
        
        document.add(infoTable);

        // Tracking Info
        Paragraph tracking = new Paragraph("Numéro de suivi: " + dto.getNumeroSuivi(), boldNormalFont);
        tracking.setSpacingAfter(10f);
        document.add(tracking);

        // Table des produits
        PdfPTable productTable = new PdfPTable(4);
        productTable.setWidthPercentage(100);
        productTable.setWidths(new float[]{4, 1, 1.5f, 1.5f});
        productTable.setSpacingBefore(10f);
        
        // Headers
        String[] headers = {"Produit", "Qté", "Poids (kg)", "Total (DT)"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, labelFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setPadding(5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            productTable.addCell(cell);
        }

        for (BonLivraisonDTO.ProduitDTO p : dto.getProduits()) {
            productTable.addCell(new PdfPCell(new Phrase(p.getNom(), normalFont)));
            
            PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(p.getQuantite()), normalFont));
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            productTable.addCell(qtyCell);
            
            PdfPCell weightCell = new PdfPCell(new Phrase(p.getPoidsUnitaire().toString(), normalFont));
            weightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            productTable.addCell(weightCell);
            
            PdfPCell priceCell = new PdfPCell(new Phrase(p.getSousTotal().toString(), normalFont));
            priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            productTable.addCell(priceCell);
        }
        document.add(productTable);

        // Totals & COD
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(100);
        totalTable.setSpacingBefore(10f);
        
        PdfPCell weightTotalCell = new PdfPCell(new Phrase("Poids Total: " + dto.getPoidsTotal() + " kg", normalFont));
        weightTotalCell.setBorder(Rectangle.NO_BORDER);
        totalTable.addCell(weightTotalCell);
        
        PdfPCell amountCell = new PdfPCell();
        amountCell.setBorder(Rectangle.NO_BORDER);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph totalPara = new Paragraph("MONTANT À PERCEVOIR (COD):", smallFont);
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        amountCell.addElement(totalPara);
        Paragraph pricePara = new Paragraph(dto.getMontantCOD().toString() + " DT", titleFont);
        pricePara.setAlignment(Element.ALIGN_RIGHT);
        amountCell.addElement(pricePara);
        totalTable.addCell(amountCell);
        
        document.add(totalTable);

        // Amount in letters
        Paragraph inWords = new Paragraph("Arrêté le présent bon à la somme de : ", normalFont);
        inWords.add(new Chunk(dto.getMontantEnLettres(), boldNormalFont));
        inWords.setSpacingBefore(5f);
        document.add(inWords);

        // QR Code and Signatures Section
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setSpacingBefore(30f);
        
        // QR Code
        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 150, 150);
        ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrBaos);
        Image qrImage = Image.getInstance(qrBaos.toByteArray());
        qrImage.setAlignment(Element.ALIGN_CENTER);
        qrCell.addElement(qrImage);
        qrCell.addElement(new Paragraph("SCANNEZ POUR VALIDER", smallFont));
        footerTable.addCell(qrCell);
        
        // Signatures
        PdfPCell sigCell = new PdfPCell();
        sigCell.setBorder(Rectangle.NO_BORDER);
        sigCell.addElement(new Paragraph("SIGNATURE DU CLIENT", smallFont));
        sigCell.addElement(new Paragraph("\n\n\n__________________________", normalFont));
        footerTable.addCell(sigCell);
        
        document.add(footerTable);

        document.close();
        return baos.toByteArray();
    }

    /**
     * Valide la livraison via le token scanné pour une commande spécifique.
     */
    @Transactional
    public Order validateDelivery(Long orderId, String token, Long clientId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée"));

        if (!order.getClient().getId().equals(clientId)) {
            throw new SecurityException("Vous n'êtes pas autorisé à valider cette livraison");
        }

        if (order.getDeliveryToken() == null || !order.getDeliveryToken().equals(token)) {
            throw new IllegalArgumentException("Token de livraison invalide ou déjà utilisé");
        }

        if (order.getDeliveryTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Le token de livraison a expiré");
        }

        if (order.getStatut() == OrderStatus.DELIVERED) {
            return order;
        }

        order.setStatut(OrderStatus.DELIVERED);
        order.setDeliveryToken(null);
        order.setDeliveryTokenExpiry(null);
        
        return orderRepository.save(order);
    }
}

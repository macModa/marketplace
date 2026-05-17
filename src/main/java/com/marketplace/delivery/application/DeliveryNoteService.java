package com.marketplace.delivery.application;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.marketplace.delivery.domain.Parcel;
import com.marketplace.delivery.infrastructure.ParcelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DeliveryNoteService {

    private final ParcelRepository parcelRepository;

    public byte[] generateDeliveryNotePdf(Long orderId) {
        // Récupérer le colis lié à la commande
        Parcel parcel = parcelRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun colis trouvé pour la commande #" + orderId));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── En-tête ──────────────────────────────────────────────────
            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 13, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

            Paragraph title = new Paragraph("BON DE LIVRAISON", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            doc.add(new Paragraph("Marchi Marketplace", new Font(Font.HELVETICA, 12, Font.ITALIC)));
            doc.add(Chunk.NEWLINE);

            // ── Infos commande ────────────────────────────────────────────
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10f);

            addRow(infoTable, "Commande N°", "#" + orderId, sectionFont, normalFont);
            addRow(infoTable, "N° de suivi", parcel.getTrackingNumber(), sectionFont, normalFont);
            addRow(infoTable, "Statut", parcel.getStatus().name(), sectionFont, normalFont);
            addRow(infoTable, "Livraison estimée",
                    parcel.getEstimatedDelivery() != null
                            ? parcel.getEstimatedDelivery()
                                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "N/A",
                    sectionFont, normalFont);

            doc.add(infoTable);
            doc.add(Chunk.NEWLINE);

            // ── Destinataire ──────────────────────────────────────────────
            Paragraph recipientTitle = new Paragraph("Destinataire", sectionFont);
            doc.add(recipientTitle);
            doc.add(Chunk.NEWLINE);

            PdfPTable recipientTable = new PdfPTable(2);
            recipientTable.setWidthPercentage(100);
            addRow(recipientTable, "Nom", parcel.getRecipientName(), sectionFont, normalFont);
            addRow(recipientTable, "Téléphone", parcel.getRecipientPhone(), sectionFont, normalFont);
            addRow(recipientTable, "Adresse", parcel.getDeliveryAddress(), sectionFont, normalFont);
            addRow(recipientTable, "Code postal", parcel.getPostalCode(), sectionFont, normalFont);
            doc.add(recipientTable);
            doc.add(Chunk.NEWLINE);

            // ── Hub & Relay ────────────────────────────────────────────────
            if (parcel.getHub() != null) {
                Paragraph hubTitle = new Paragraph("Centre de distribution", sectionFont);
                doc.add(hubTitle);
                doc.add(Chunk.NEWLINE);
                PdfPTable hubTable = new PdfPTable(2);
                hubTable.setWidthPercentage(100);
                addRow(hubTable, "Hub", parcel.getHub().getName(), sectionFont, normalFont);
                if (parcel.getRelayPoint() != null) {
                    addRow(hubTable, "Point relais",
                            parcel.getRelayPoint().getName(), sectionFont, normalFont);
                }
                doc.add(hubTable);
                doc.add(Chunk.NEWLINE);
            }

            // ── Pied de page ──────────────────────────────────────────────
            doc.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph(
                    "Document généré automatiquement par Marchi Marketplace.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF: " + e.getMessage(), e);
        }
    }

    private void addRow(PdfPTable table, String label, String value,
                        Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(new java.awt.Color(245, 245, 245));

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setPadding(6);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}

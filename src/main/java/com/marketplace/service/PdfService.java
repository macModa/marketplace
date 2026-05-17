package com.marketplace.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.marketplace.entity.Order;
import com.marketplace.entity.OrderLine;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateOrderPdf(Order order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            document.add(new Paragraph("BON DE LIVRAISON"));
            document.add(new Paragraph("Commande #" + order.getId()));
            document.add(new Paragraph("Client: " + (order.getClient() != null ? order.getClient().getEmail() : "")));
            document.add(new Paragraph("Artisan: " + (order.getArtisan() != null ? order.getArtisan().getEmail() : "")));
            document.add(new Paragraph("Total: " + order.getTotal() + " TND"));

            document.add(new Paragraph("Produits:"));

            if (order.getOrderLines() == null || order.getOrderLines().isEmpty()) {
                throw new RuntimeException("Commande sans articles — PDF non générable");
            }

            System.out.println("ITEMS SIZE = " + order.getOrderLines().size());
            for (OrderLine item : order.getOrderLines()) {
                String productName = item.getProduct() != null ? item.getProduct().getNom() : "";
                document.add(new Paragraph(productName + " x" + item.getQuantite()));
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
        
        return out.toByteArray();
    }
}

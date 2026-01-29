package com.marketplace.dto;

import java.math.BigDecimal;

public class ProductResponseDto {

    private Long id;
    private String nom;
    private String description;
    private BigDecimal prix;
    private Integer stock;
    private Long artisanId;
    private Long categoryId;
    private boolean available;
    private String artisanNom;
    private String categoryNom;

    public ProductResponseDto(
            Long id,
            String nom,
            String description,
            BigDecimal prix,
            Integer stock,
            Long artisanId,
            Long categoryId,
            boolean available,
            String artisanNom,
            String categoryNom
    ) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.stock = stock;
        this.artisanId = artisanId;
        this.categoryId = categoryId;
        this.available = available;
        this.artisanNom = artisanNom;
        this.categoryNom = categoryNom;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public BigDecimal getPrix() { return prix; }
    public Integer getStock() { return stock; }
    public Long getArtisanId() { return artisanId; }
    public Long getCategoryId() { return categoryId; }
    public boolean isAvailable() { return available; }
    public String getArtisanNom() { return artisanNom; }
    public String getCategoryNom() { return categoryNom; }
}

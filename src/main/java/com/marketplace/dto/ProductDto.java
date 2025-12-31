package com.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    
    @NotBlank(message = "Le nom du produit est requis")
    private String nom;
    
    private String description;
    
    @NotNull(message = "Le prix est requis")
    @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à 0")
    private BigDecimal prix;
    
    @NotNull(message = "Le stock est requis")
    @Min(value = 0, message = "Le stock ne peut pas être négatif")
    private Integer stock;
    
    @NotNull(message = "L'artisan est requis")
    private Long artisanId;
    
    @NotNull(message = "La catégorie est requise")
    private Long categoryId;
    
    private String artisanNom;
    private String categoryNom;
}


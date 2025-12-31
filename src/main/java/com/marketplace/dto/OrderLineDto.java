package com.marketplace.dto;

import jakarta.validation.constraints.Min;
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
public class OrderLineDto {
    private Long id;
    
    @NotNull(message = "Le produit est requis")
    private Long productId;
    
    @NotNull(message = "La quantité est requise")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;
    
    private BigDecimal prixUnitaire;
    private BigDecimal subtotal;
    private String productNom;
}


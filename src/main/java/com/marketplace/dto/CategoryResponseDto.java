package com.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de réponse pour les catégories.
 * Découple la couche de persistance (entité JPA) de l'API exposée.
 * Le productCount est calculé dans la transaction (@Transactional) avant
 * la fermeture de la session Hibernate, évitant toute LazyInitializationException.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {

    private Long id;
    private String nom;
    private int productCount;
}

package com.marketplace.service;

import com.marketplace.dto.CategoryDto;
import com.marketplace.entity.Category;
import com.marketplace.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    
    private final CategoryRepository categoryRepository;
    
    @Transactional
    public Category createCategory(CategoryDto categoryDto) {
        logger.info("Création d'une nouvelle catégorie: {}", categoryDto.getNom());
        
        if (categoryRepository.existsByNom(categoryDto.getNom())) {
            throw new IllegalArgumentException("Une catégorie avec ce nom existe déjà");
        }
        
        Category category = new Category();
        category.setNom(categoryDto.getNom());
        
        Category saved = categoryRepository.save(category);
        logger.info("Catégorie créée avec succès: ID {}", saved.getId());
        
        return saved;
    }
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + id));
    }
    
    @Transactional
    public Category updateCategory(Long id, CategoryDto categoryDto) {
        logger.info("Mise à jour de la catégorie: {}", id);
        
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + id));
        
        if (!category.getNom().equals(categoryDto.getNom()) && 
            categoryRepository.existsByNom(categoryDto.getNom())) {
            throw new IllegalArgumentException("Une catégorie avec ce nom existe déjà");
        }
        
        category.setNom(categoryDto.getNom());
        
        return categoryRepository.save(category);
    }
    
    @Transactional
    public void deleteCategory(Long id) {
        logger.info("Suppression de la catégorie: {}", id);
        
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + id));
        
        if (category.getProductCount() > 0) {
            throw new IllegalStateException("Impossible de supprimer une catégorie contenant des produits");
        }
        
        categoryRepository.delete(category);
        logger.info("Catégorie supprimée avec succès: {}", id);
    }
}


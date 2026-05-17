package com.marketplace.service;

import com.marketplace.dto.CategoryDto;
import com.marketplace.dto.CategoryResponseDto;
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
    public CategoryResponseDto createCategory(CategoryDto categoryDto) {
        logger.info("Création d'une nouvelle catégorie: {}", categoryDto.getNom());
        
        if (categoryRepository.existsByNom(categoryDto.getNom())) {
            throw new IllegalArgumentException("Une catégorie avec ce nom existe déjà");
        }
        
        Category category = new Category();
        category.setNom(categoryDto.getNom());
        
        Category saved = categoryRepository.save(category);
        logger.info("Catégorie créée avec succès: ID {}", saved.getId());
        
        return mapToResponseDto(saved);
    }
    
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
            .map(this::mapToResponseDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        return categoryRepository.findById(id)
            .map(this::mapToResponseDto)
            .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + id));
    }
    
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryDto categoryDto) {
        logger.info("Mise à jour de la catégorie: {}", id);
        
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + id));
        
        if (!category.getNom().equals(categoryDto.getNom()) && 
            categoryRepository.existsByNom(categoryDto.getNom())) {
            throw new IllegalArgumentException("Une catégorie avec ce nom existe déjà");
        }
        
        category.setNom(categoryDto.getNom());
        Category updated = categoryRepository.save(category);
        
        return mapToResponseDto(updated);
    }
    
    @Transactional
    public void deleteCategory(Long id) {
        logger.info("Suppression de la catégorie: {}", id);
        
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + id));
        
        // Accès à products.size() dans une méthode @Transactional
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer une catégorie contenant des produits");
        }
        
        categoryRepository.delete(category);
        logger.info("Catégorie supprimée avec succès: {}", id);
    }

    private CategoryResponseDto mapToResponseDto(Category category) {
        return new CategoryResponseDto(
            category.getId(),
            category.getNom(),
            category.getProducts() != null ? category.getProducts().size() : 0
        );
    }
}


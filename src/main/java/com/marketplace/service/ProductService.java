package com.marketplace.service;

import com.marketplace.dto.ProductDto;
import com.marketplace.entity.Artisan;
import com.marketplace.entity.Category;
import com.marketplace.entity.Product;
import com.marketplace.repository.ArtisanRepository;
import com.marketplace.repository.CategoryRepository;
import com.marketplace.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    private final ProductRepository productRepository;
    private final ArtisanRepository artisanRepository;
    private final CategoryRepository categoryRepository;
    
    @Transactional
    public Product createProduct(ProductDto productDto, Long artisanId) {
        logger.info("Création d'un nouveau produit: {} par l'artisan: {}", productDto.getNom(), artisanId);
        
        Artisan artisan = artisanRepository.findById(artisanId)
            .orElseThrow(() -> new IllegalArgumentException("Artisan non trouvé: " + artisanId));
        
        Category category = categoryRepository.findById(productDto.getCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + productDto.getCategoryId()));
        
        Product product = new Product();
        product.setNom(productDto.getNom());
        product.setDescription(productDto.getDescription());
        product.setPrix(productDto.getPrix());
        product.setStock(productDto.getStock());
        product.setArtisan(artisan);
        product.setCategory(category);
        
        Product saved = productRepository.save(product);
        logger.info("Produit créé avec succès: ID {}", saved.getId());
        
        return saved;
    }
    
    @Transactional
    public Product updateProduct(Long productId, ProductDto productDto, Long artisanId) {
        logger.info("Mise à jour du produit: {} par l'artisan: {}", productId, artisanId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + productId));
        
        if (!product.getArtisan().getId().equals(artisanId)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à modifier ce produit");
        }
        
        Category category = categoryRepository.findById(productDto.getCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + productDto.getCategoryId()));
        
        product.setNom(productDto.getNom());
        product.setDescription(productDto.getDescription());
        product.setPrix(productDto.getPrix());
        product.setStock(productDto.getStock());
        product.setCategory(category);
        
        return productRepository.save(product);
    }
    
    @Transactional
    public void deleteProduct(Long productId, Long artisanId) {
        logger.info("Suppression du produit: {} par l'artisan: {}", productId, artisanId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + productId));
        
        if (!product.getArtisan().getId().equals(artisanId)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à supprimer ce produit");
        }
        
        productRepository.delete(product);
        logger.info("Produit supprimé avec succès: {}", productId);
    }
    
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
    
    public Product getProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + id));
    }
    
    public Page<Product> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }
    
    public Page<Product> getProductsByArtisan(Long artisanId, Pageable pageable) {
        return productRepository.findByArtisanId(artisanId, pageable);
    }
    
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchProducts(keyword, pageable);
    }
    
    public Page<Product> getAvailableProducts(Pageable pageable) {
        return productRepository.findAvailableProducts(pageable);
    }
    
    @Transactional
    public void updateStock(Long productId, int quantity) {
        logger.info("Mise à jour du stock pour le produit: {}, quantité: {}", productId, quantity);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + productId));
        
        if (quantity > 0) {
            product.increaseStock(quantity);
        } else if (quantity < 0) {
            product.decreaseStock(Math.abs(quantity));
        }
        
        productRepository.save(product);
    }
}


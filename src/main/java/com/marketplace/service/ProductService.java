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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.marketplace.dto.ProductResponseDto;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.StandardCopyOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

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
        product.setImageUrl(productDto.getImageUrl());

        logger.info("Setting imageUrl on entity: {}", productDto.getImageUrl());
        logger.info("Entity imageUrl after set: {}", product.getImageUrl());

        Product saved = productRepository.save(product);
        logger.info("Produit créé avec succès: ID {}, imageUrl: {}", saved.getId(), saved.getImageUrl());
        
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
        product.setImageUrl(productDto.getImageUrl());
        
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
    
    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit non trouvé: " + id));
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(Long categoryId, Pageable pageable) {
        // Verify category exists — prevents 500 crash when category ID doesn't exist in Railway DB
        boolean categoryExists = categoryRepository.existsById(categoryId);
        if (!categoryExists) {
            logger.warn("Category not found with id: {} — returning empty page", categoryId);
            // Return empty page instead of crashing with 500
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        return productRepository.findByCategoryId(categoryId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getProductsByArtisan(Long artisanId, Pageable pageable) {
        return productRepository.findByArtisanId(artisanId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchProducts(keyword, pageable);
    }
    
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public ProductResponseDto toResponseDto(Product product) {
        // Safe null-checks prevent NullPointerException on lazy-loaded relations
        Long artisanId = null;
        String artisanNom = null;
        Long categoryId = null;
        String categoryNom = null;

        try {
            if (product.getArtisan() != null) {
                artisanId = product.getArtisan().getId();
                artisanNom = product.getArtisan().getNomBoutique();
            }
        } catch (Exception e) {
            logger.warn("Could not load artisan for product {}: {}", product.getId(), e.getMessage());
        }

        try {
            if (product.getCategory() != null) {
                categoryId = product.getCategory().getId();
                categoryNom = product.getCategory().getNom();
            }
        } catch (Exception e) {
            logger.warn("Could not load category for product {}: {}", product.getId(), e.getMessage());
        }

        return new ProductResponseDto(
                product.getId(),
                product.getNom(),
                product.getDescription(),
                product.getPrix(),
                product.getStock(),
                artisanId,
                categoryId,
                product.isAvailable(),
                artisanNom,
                categoryNom,
                product.getImageUrl()
        );
    }
    @Transactional
    public String uploadImage(Long productId, MultipartFile file, Long artisanId)
            throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produit introuvable"));

        if (!product.getArtisan().getId().equals(artisanId)) {
            throw new IllegalStateException("Accès refusé");
        }

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get("uploads/products/" + filename);

        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        String imageUrl = "/uploads/products/" + filename;
        product.setImageUrl(imageUrl);

        productRepository.save(product);
        return imageUrl;
    }
    public String uploadProductImage(Long productId,
                                     MultipartFile file,
                                     Long artisanId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        if (!product.getArtisan().getId().equals(artisanId)) {
            throw new RuntimeException("Non autorisé");
        }

        try {

            String uploadDir = "uploads/products/";

            Files.createDirectories(Paths.get(uploadDir));

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();

            Path filePath = Paths.get(uploadDir + filename);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "http://localhost:8080/uploads/products/" + filename;

            product.setImageUrl(imageUrl);

            productRepository.save(product);

            return imageUrl;

        } catch (Exception e) {
            throw new RuntimeException("Erreur upload image");
        }
    }

}


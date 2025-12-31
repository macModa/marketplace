package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.ProductDto;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    
    private final ProductService productService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Product>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<Product>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    @GetMapping("/artisan/{artisanId}")
    public ResponseEntity<ApiResponse<Page<Product>>> getProductsByArtisan(
            @PathVariable Long artisanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getProductsByArtisan(artisanId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<Product>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Page<Product>>> getAvailableProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getAvailableProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ARTISAN')")
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @Valid @RequestBody ProductDto productDto,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Product product = productService.createProduct(productDto, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Produit créé avec succès", product));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ARTISAN')")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDto productDto,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Product product = productService.updateProduct(id, productDto, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Produit mis à jour avec succès", product));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ARTISAN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        productService.deleteProduct(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Produit supprimé avec succès", null));
    }
}


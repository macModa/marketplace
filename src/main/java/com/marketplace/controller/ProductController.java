package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.ProductDto;
import com.marketplace.dto.ProductResponseDto;
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
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;

    // =========================
    // GET ALL PRODUCTS (PUBLIC)
    // =========================
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<ProductResponseDto> products = productService
                .getAllProducts(pageable)
                .map(productService::toResponseDto);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // =========================
    // GET PRODUCT BY ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(
                ApiResponse.success(productService.toResponseDto(product))
        );
    }

    // =========================
    // GET BY CATEGORY
    // =========================
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<ProductResponseDto> products = productService
                .getProductsByCategory(categoryId, pageable)
                .map(productService::toResponseDto);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // =========================
    // GET BY ARTISAN
    // =========================
    @GetMapping("/artisan/{artisanId}")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getProductsByArtisan(
            @PathVariable Long artisanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<ProductResponseDto> products = productService
                .getProductsByArtisan(artisanId, pageable)
                .map(productService::toResponseDto);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // =========================
    // SEARCH
    // =========================
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<ProductResponseDto> products = productService
                .searchProducts(keyword, pageable)
                .map(productService::toResponseDto);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // =========================
    // AVAILABLE PRODUCTS
    // =========================
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getAvailableProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<ProductResponseDto> products = productService
                .getAvailableProducts(pageable)
                .map(productService::toResponseDto);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // =========================
    // CREATE PRODUCT (ARTISAN)
    // =========================
    @PostMapping
    @PreAuthorize("hasRole('ARTISAN')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @Valid @RequestBody ProductDto productDto,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        Product product = productService.createProduct(productDto, user.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Produit créé avec succès",
                        productService.toResponseDto(product)
                ));
    }

    // =========================
    // UPDATE PRODUCT (ARTISAN)
    // =========================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ARTISAN')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDto productDto,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        Product product = productService.updateProduct(id, productDto, user.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Produit mis à jour avec succès",
                        productService.toResponseDto(product)
                )
        );
    }

    // =========================
    // DELETE PRODUCT (ARTISAN)
    // =========================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ARTISAN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        productService.deleteProduct(id, user.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Produit supprimé avec succès", null)
        );
    }
    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ARTISAN')")
    public ResponseEntity<ApiResponse<String>> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image,
            Authentication authentication) throws IOException {

        User user = (User) authentication.getPrincipal();

        String imageUrl = productService.uploadImage(id, image, user.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Image uploadée avec succès", imageUrl)
        );
    }

}

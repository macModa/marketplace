package com.marketplace.service;

import com.marketplace.dto.ProductDto;
import com.marketplace.entity.Artisan;
import com.marketplace.entity.Product;
import com.marketplace.repository.ArtisanRepository;
import com.marketplace.repository.CategoryRepository;
import com.marketplace.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository productRepository;
    private ArtisanRepository artisanRepository;
    private CategoryRepository categoryRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        artisanRepository = mock(ArtisanRepository.class);
        categoryRepository = mock(CategoryRepository.class);

        productService = new ProductService(
                productRepository,
                artisanRepository,
                categoryRepository
        );
    }

    @Test
    void getProductById_whenProductDoesNotExist_returns404() {
        Long productId = 999L;

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProductById(productId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productRepository).findById(productId);
        verifyNoInteractions(artisanRepository, categoryRepository);
    }

    @Test
    void updateProduct_whenAnotherArtisanOwnsProduct_returns403() {
        Long productId = 10L;
        Long ownerArtisanId = 1L;
        Long requestingArtisanId = 2L;

        Artisan owner = mock(Artisan.class);
        Product product = mock(Product.class);

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
        when(product.getArtisan()).thenReturn(owner);
        when(owner.getId()).thenReturn(ownerArtisanId);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.updateProduct(
                        productId,
                        mock(ProductDto.class),
                        requestingArtisanId
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(categoryRepository);
    }
}
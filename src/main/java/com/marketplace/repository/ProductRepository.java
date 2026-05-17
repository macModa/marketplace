package com.marketplace.repository;

import com.marketplace.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // JOIN FETCH prevents LazyInitializationException when open-in-view=false
    @Query("SELECT p FROM Product p JOIN FETCH p.artisan JOIN FETCH p.category WHERE p.category.id = :categoryId")
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
    
    @Query("SELECT p FROM Product p JOIN FETCH p.artisan JOIN FETCH p.category WHERE p.artisan.id = :artisanId")
    Page<Product> findByArtisanId(@Param("artisanId") Long artisanId, Pageable pageable);
    
    @Query("SELECT p FROM Product p JOIN FETCH p.artisan JOIN FETCH p.category WHERE p.stock > 0")
    Page<Product> findAvailableProducts(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.stock = 0")
    Page<Product> findOutOfStockProducts(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.prix BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE LOWER(p.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.artisan.id = :artisanId AND p.stock < :threshold")
    List<Product> findLowStockProductsByArtisan(@Param("artisanId") Long artisanId,
                                                @Param("threshold") Integer threshold);
}


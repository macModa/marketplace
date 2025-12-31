package com.marketplace.repository;

import com.marketplace.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNom(String nom);
    boolean existsByNom(String nom);
}


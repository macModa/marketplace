package com.marketplace.repository;

import com.marketplace.entity.Artisan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtisanRepository extends JpaRepository<Artisan, Long> {
    Optional<Artisan> findByEmail(String email);
    
    @Query("SELECT a FROM Artisan a WHERE a.verifie = true")
    List<Artisan> findVerifiedArtisans();
    
    @Query("SELECT a FROM Artisan a WHERE a.verifie = false")
    List<Artisan> findUnverifiedArtisans();
    
    boolean existsByNomBoutique(String nomBoutique);
}


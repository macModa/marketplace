package com.marketplace.config;

import com.marketplace.entity.Category;
import com.marketplace.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CategoryInitializer {

    private static final Logger logger = LoggerFactory.getLogger(CategoryInitializer.class);
    private final CategoryRepository categoryRepository;

    @Bean
    public CommandLineRunner initCategories() {
        return args -> {
            if (categoryRepository.count() == 0) {
                logger.info("🛠️ Base de données vide sur Railway. Initialisation des catégories par défaut...");
                
                List<String> defaultCategories = Arrays.asList(
                    "Décoration", 
                    "Bijoux", 
                    "Vêtements", 
                    "Poterie", 
                    "Tissage",
                    "Épicerie Fine",
                    "Cosmétique Naturelle"
                );

                for (String nom : defaultCategories) {
                    Category category = new Category();
                    category.setNom(nom);
                    categoryRepository.save(category);
                }
                
                logger.info("✅ {} catégories initialisées avec succès.", defaultCategories.size());
            } else {
                logger.info("ℹ️ Catégories déjà présentes en base de données (Count: {})", categoryRepository.count());
            }
        };
    }
}

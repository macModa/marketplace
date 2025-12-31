package com.marketplace.service;

import com.marketplace.entity.Artisan;
import com.marketplace.entity.Client;
import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.repository.ArtisanRepository;
import com.marketplace.repository.ClientRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final ArtisanRepository artisanRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public User registerUser(String nom, String email, String password, String telephone, 
                            String ville, Role role, String nomBoutique, String description, 
                            String adresseLivraison) {
        logger.info("Tentative d'inscription d'un nouvel utilisateur: {}", email);
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }
        
        User user;
        
        if (role == Role.ARTISAN) {
            if (nomBoutique == null || nomBoutique.isBlank()) {
                throw new IllegalArgumentException("Le nom de la boutique est requis pour un artisan");
            }
            if (artisanRepository.existsByNomBoutique(nomBoutique)) {
                throw new IllegalArgumentException("Une boutique avec ce nom existe déjà");
            }
            
            Artisan artisan = new Artisan();
            artisan.setNom(nom);
            artisan.setEmail(email);
            artisan.setPassword(passwordEncoder.encode(password));
            artisan.setTelephone(telephone);
            artisan.setVille(ville);
            artisan.setRole(role);
            artisan.setNomBoutique(nomBoutique);
            artisan.setDescription(description);
            artisan.setVerifie(false);
            
            user = artisanRepository.save(artisan);
            logger.info("Artisan créé avec succès: {}", email);
            
        } else if (role == Role.CLIENT) {
            Client client = new Client();
            client.setNom(nom);
            client.setEmail(email);
            client.setPassword(passwordEncoder.encode(password));
            client.setTelephone(telephone);
            client.setVille(ville);
            client.setRole(role);
            client.setAdresseLivraison(adresseLivraison);
            
            user = clientRepository.save(client);
            logger.info("Client créé avec succès: {}", email);
            
        } else {
            throw new IllegalArgumentException("Rôle non supporté: " + role);
        }
        
        return user;
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + email));
    }
    
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + id));
    }
}


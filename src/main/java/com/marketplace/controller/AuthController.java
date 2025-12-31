package com.marketplace.controller;

import com.marketplace.dto.AuthRequest;
import com.marketplace.dto.AuthResponse;
import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.RegisterRequest;
import com.marketplace.entity.User;
import com.marketplace.security.JwtTokenProvider;
import com.marketplace.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Tentative d'inscription: {}", request.getEmail());
        
        User user = userService.registerUser(
            request.getNom(),
            request.getEmail(),
            request.getPassword(),
            request.getTelephone(),
            request.getVille(),
            request.getRole(),
            request.getNomBoutique(),
            request.getDescription(),
            request.getAdresseLivraison()
        );
        
        // Auto login after registration
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        String token = tokenProvider.generateToken(authentication);
        
        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(token);
        authResponse.setUserId(user.getId());
        authResponse.setEmail(user.getEmail());
        authResponse.setRole(user.getRole().name());
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Inscription réussie", authResponse));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        logger.info("Tentative de connexion: {}", request.getEmail());
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String token = tokenProvider.generateToken(authentication);
        User user = userService.findByEmail(request.getEmail());
        
        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(token);
        authResponse.setUserId(user.getId());
        authResponse.setEmail(user.getEmail());
        authResponse.setRole(user.getRole().name());
        
        return ResponseEntity.ok(ApiResponse.success("Connexion réussie", authResponse));
    }
}


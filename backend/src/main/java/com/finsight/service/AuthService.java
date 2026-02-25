package com.finsight.service;

import com.finsight.dto.AuthResponse;
import com.finsight.dto.LoginRequest;
import com.finsight.dto.SignupRequest;
import com.finsight.model.User;
import com.finsight.repository.UserRepository;
import com.finsight.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final DemoDataService demoDataService;
    
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .build();
        
        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());
        
        // Generate demo data for new user
        try {
            int transactionsCreated = demoDataService.seedUserIfEmpty(user.getId());
            log.info("Generated {} demo transactions for new user: {}", transactionsCreated, user.getId());
        } catch (Exception e) {
            log.warn("Failed to generate demo data for user {}: {}", user.getId(), e.getMessage());
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .message("User registered successfully")
                .build();
    }
    
    public AuthResponse login(LoginRequest request) {
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        
        log.info("User logged in: {}", user.getUsername());
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .message("Login successful")
                .build();
    }
}

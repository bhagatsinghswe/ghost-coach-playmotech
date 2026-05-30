package com.playmotech.ghostcoach.service;

import com.playmotech.ghostcoach.dto.request.LoginRequest;
import com.playmotech.ghostcoach.dto.request.RegisterRequest;
import com.playmotech.ghostcoach.dto.response.AuthResponse;
import com.playmotech.ghostcoach.entity.User;
import com.playmotech.ghostcoach.exception.ConflictException;
import com.playmotech.ghostcoach.repository.UserRepository;
import com.playmotech.ghostcoach.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = User.builder()
                .fullName(req.getFullName().trim())
                .email(req.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(req.getPassword()))
                .sport(req.getSport())
                .position(req.getPosition().trim())
                .experienceLevel(req.getExperienceLevel())
                .age(req.getAge())
                .build();

        userRepository.save(user);
        log.info("New player registered: {} ({})", user.getEmail(), user.getSport());

        String token = generateToken(user.getEmail());
        return buildResponse(user, token);
    }

    public AuthResponse login(LoginRequest req) {
        // Spring Security throws BadCredentialsException on failure — handled globally
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getEmail().toLowerCase(), req.getPassword()));

        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found after successful auth"));

        String token = generateToken(user.getEmail());
        return buildResponse(user, token);
    }

    private String generateToken(String email) {
        UserDetails details = userDetailsService.loadUserByUsername(email);
        return jwtUtils.generateToken(details);
    }

    private AuthResponse buildResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .sport(user.getSport())
                .position(user.getPosition())
                .experienceLevel(user.getExperienceLevel())
                .age(user.getAge())
                .build();
    }
}

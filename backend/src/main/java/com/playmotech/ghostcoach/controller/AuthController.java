package com.playmotech.ghostcoach.controller;

import com.playmotech.ghostcoach.dto.request.LoginRequest;
import com.playmotech.ghostcoach.dto.request.RegisterRequest;
import com.playmotech.ghostcoach.dto.response.ApiResponse;
import com.playmotech.ghostcoach.dto.response.AuthResponse;
import com.playmotech.ghostcoach.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Creates a new player account and returns a JWT.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created successfully", response));
    }

    /**
     * POST /api/auth/login
     * Authenticates a player and returns a fresh JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }
}

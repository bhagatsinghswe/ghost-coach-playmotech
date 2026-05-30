package com.playmotech.ghostcoach.controller;

import com.playmotech.ghostcoach.dto.request.ProfileUpdateRequest;
import com.playmotech.ghostcoach.dto.response.ApiResponse;
import com.playmotech.ghostcoach.dto.response.AuthResponse;
import com.playmotech.ghostcoach.entity.User;
import com.playmotech.ghostcoach.exception.ResourceNotFoundException;
import com.playmotech.ghostcoach.repository.CoachingSessionRepository;
import com.playmotech.ghostcoach.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final CoachingSessionRepository sessionRepository;

    /**
     * GET /api/profile
     * Returns the authenticated player's full profile + aggregate stats.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(
            @AuthenticationPrincipal UserDetails principal) {

        User user = loadUser(principal.getUsername());
        long totalSessions = sessionRepository.countByUser(user);

        Map<String, Object> profile = Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "sport", user.getSport(),
                "position", user.getPosition(),
                "experienceLevel", user.getExperienceLevel(),
                "age", user.getAge() != null ? user.getAge() : "",
                "totalSessions", totalSessions,
                "memberSince", user.getCreatedAt()
        );
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    /**
     * PATCH /api/profile
     * Partial update — only non-null fields are applied.
     * Allows players to update sport, position, level as they progress.
     */
    @PatchMapping
    @Transactional
    public ResponseEntity<ApiResponse<AuthResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        User user = loadUser(principal.getUsername());

        if (request.getFullName() != null)        user.setFullName(request.getFullName().trim());
        if (request.getSport() != null)           user.setSport(request.getSport());
        if (request.getPosition() != null)        user.setPosition(request.getPosition().trim());
        if (request.getExperienceLevel() != null) user.setExperienceLevel(request.getExperienceLevel());
        if (request.getAge() != null)             user.setAge(request.getAge());

        userRepository.save(user);

        AuthResponse updated = AuthResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .sport(user.getSport())
                .position(user.getPosition())
                .experienceLevel(user.getExperienceLevel())
                .age(user.getAge())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Profile updated", updated));
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}

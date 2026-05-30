package com.playmotech.ghostcoach.controller;

import com.playmotech.ghostcoach.dto.response.*;
import com.playmotech.ghostcoach.service.CoachingSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final CoachingSessionService sessionService;

    /**
     * POST /api/sessions/upload
     * Accepts a stance image, persists it, triggers async AI analysis.
     * Returns immediately with a PENDING session record.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<SessionResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) throws IOException {

        String baseUrl = getBaseUrl(request);
        SessionResponse session = sessionService.uploadAndAnalyze(
                file, principal.getUsername(), baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Image uploaded — AI analysis in progress", session));
    }

    /**
     * GET /api/sessions
     * Paginated session history for the authenticated player.
     * Query params: page (default 0), size (default 10)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SessionSummary>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        String baseUrl = getBaseUrl(request);
        Page<SessionSummary> history = sessionService.getHistory(
                principal.getUsername(), page, size, baseUrl);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    /**
     * GET /api/sessions/{id}
     * Full session detail including complete AI feedback.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        String baseUrl = getBaseUrl(request);
        SessionResponse session = sessionService.getSession(
                id, principal.getUsername(), baseUrl);
        return ResponseEntity.ok(ApiResponse.ok(session));
    }

    /**
     * GET /api/sessions/progress
     * Score over time for the progress chart (bonus feature).
     */
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<List<ScoreProgressPoint>>> getProgress(
            @AuthenticationPrincipal UserDetails principal) {

        List<ScoreProgressPoint> progress = sessionService.getProgress(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(progress));
    }

    // ---- helper ----

    private String getBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                        ? ":" + request.getServerPort() : "");
    }
}

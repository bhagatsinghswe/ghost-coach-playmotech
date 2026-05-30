package com.playmotech.ghostcoach.controller;

import com.playmotech.ghostcoach.dto.request.ChatMessageRequest;
import com.playmotech.ghostcoach.dto.response.ApiResponse;
import com.playmotech.ghostcoach.dto.response.ChatMessageResponse;
import com.playmotech.ghostcoach.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions/{sessionId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/sessions/{sessionId}/chat
     * Send a follow-up coaching question; AI responds with session context.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        ChatMessageResponse reply = chatService.sendMessage(
                sessionId, request.getContent(), principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(reply));
    }

    /**
     * GET /api/sessions/{sessionId}/chat
     * Retrieve the full chat history for a session.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getHistory(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails principal) {

        List<ChatMessageResponse> history = chatService.getHistory(
                sessionId, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}

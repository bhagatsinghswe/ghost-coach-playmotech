package com.playmotech.ghostcoach.service;

import com.playmotech.ghostcoach.dto.response.ChatMessageResponse;
import com.playmotech.ghostcoach.dto.response.CoachingFeedback;
import com.playmotech.ghostcoach.entity.ChatMessage;
import com.playmotech.ghostcoach.entity.CoachingSession;
import com.playmotech.ghostcoach.entity.CoachingSession.Status;
import com.playmotech.ghostcoach.entity.User;
import com.playmotech.ghostcoach.exception.BadRequestException;
import com.playmotech.ghostcoach.exception.ResourceNotFoundException;
import com.playmotech.ghostcoach.repository.ChatMessageRepository;
import com.playmotech.ghostcoach.repository.CoachingSessionRepository;
import com.playmotech.ghostcoach.util.SessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final CoachingSessionRepository sessionRepo;
    private final ChatMessageRepository chatRepo;
    private final GeminiVisionService geminiService;
    private final CoachingSessionService sessionService;
    private final SessionMapper sessionMapper;

    /**
     * Send a player message and get an AI coaching response.
     * The AI is seeded with the session's feedback for full context-awareness.
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long sessionId, String userText, String email) {
        User user = sessionService.getUser(email);
        CoachingSession session = sessionRepo.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() != Status.COMPLETED) {
            throw new BadRequestException("Chat is only available after AI analysis is complete");
        }

        // Persist the player's message
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.Role.USER)
                .content(userText)
                .build();
        chatRepo.save(userMsg);

        // Build history for context window
        List<ChatMessage> history = chatRepo.findBySessionOrderByCreatedAtAsc(session);
        List<Map<String, String>> historyMaps = history.stream()
                .map(m -> Map.of("role", m.getRole().name(), "content", m.getContent()))
                .collect(Collectors.toList());

        // Build feedback DTO for the system context
        CoachingFeedback feedback = buildFeedbackFromSession(session);

        // Call Gemini
        String aiText = geminiService.chat(userText, feedback, user, historyMaps);

        // Persist AI reply
        ChatMessage aiMsg = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.Role.ASSISTANT)
                .content(aiText)
                .build();
        chatRepo.save(aiMsg);

        return ChatMessageResponse.builder()
                .id(aiMsg.getId())
                .role(aiMsg.getRole())
                .content(aiMsg.getContent())
                .createdAt(aiMsg.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getHistory(Long sessionId, String email) {
        User user = sessionService.getUser(email);
        CoachingSession session = sessionRepo.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        return chatRepo.findBySessionOrderByCreatedAtAsc(session).stream()
                .map(m -> ChatMessageResponse.builder()
                        .id(m.getId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ---- helper ----

    private CoachingFeedback buildFeedbackFromSession(CoachingSession s) {
        return CoachingFeedback.builder()
                .overallScore(s.getOverallScore())
                .priorityFix(s.getPriorityFix())
                .drillSuggestion(s.getDrillSuggestion())
                .confidenceLevel(s.getConfidenceLevel())
                .build();
    }
}

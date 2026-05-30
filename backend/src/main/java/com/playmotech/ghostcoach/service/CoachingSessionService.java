package com.playmotech.ghostcoach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.ghostcoach.dto.response.*;
import com.playmotech.ghostcoach.entity.CoachingSession;
import com.playmotech.ghostcoach.entity.CoachingSession.Status;
import com.playmotech.ghostcoach.entity.User;
import com.playmotech.ghostcoach.exception.ResourceNotFoundException;
import com.playmotech.ghostcoach.repository.CoachingSessionRepository;
import com.playmotech.ghostcoach.repository.UserRepository;
import com.playmotech.ghostcoach.util.FileStorageUtil;
import com.playmotech.ghostcoach.util.SessionMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoachingSessionService {

    private final CoachingSessionRepository sessionRepo;
    private final UserRepository userRepo;
    private final FileStorageUtil fileStorage;
    private final GeminiVisionService geminiService;
    private final SessionMapper mapper;
    private final ObjectMapper objectMapper;

    // ---- Feature 2: Upload & trigger async AI analysis ----

    @Transactional
    public SessionResponse uploadAndAnalyze(MultipartFile file, String email, String baseUrl)
            throws IOException {
        User user = getUser(email);

        // Persist image first so player always gets a session record
        String storagePath = fileStorage.store(file, user.getId());

        CoachingSession session = CoachingSession.builder()
                .user(user)
                .originalFileName(file.getOriginalFilename())
                .storagePath(storagePath)
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .status(Status.PENDING)
                .build();

        session = sessionRepo.save(session);
        log.info("Session {} created for user {}, triggering AI analysis", session.getId(), email);

        // Run AI call asynchronously — client polls or sees PENDING status
        triggerAnalysis(session.getId(), email);

        return mapper.toResponse(session, baseUrl);
    }

    /**
     * Async AI analysis — runs in a separate thread so the upload endpoint
     * returns immediately. The session status transitions: PENDING → PROCESSING → COMPLETED|FAILED.
     */
    @Async
    @Transactional
    public void triggerAnalysis(Long sessionId, String email) {
        CoachingSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        session.setStatus(Status.PROCESSING);
        sessionRepo.save(session);

        try {
            byte[] imageBytes = fileStorage.readBytes(session.getStoragePath());
            User user = session.getUser();

            CoachingFeedback feedback = geminiService.analyzeStance(
                    imageBytes, session.getContentType(), user);

            session.setOverallScore(feedback.getOverallScore());
            session.setStrengthsJson(objectMapper.writeValueAsString(feedback.getStrengths()));
            session.setAreasToImproveJson(objectMapper.writeValueAsString(feedback.getAreasToImprove()));
            session.setPriorityFix(feedback.getPriorityFix());
            session.setDrillSuggestion(feedback.getDrillSuggestion());
            session.setConfidenceLevel(
                    CoachingSession.ConfidenceLevel.valueOf(feedback.getConfidenceLevel().name()));
            session.setStatus(Status.COMPLETED);

            log.info("AI analysis complete for session {} — score {}/10", sessionId, feedback.getOverallScore());

        } catch (Exception e) {
            log.error("AI analysis failed for session {}: {}", sessionId, e.getMessage(), e);
            session.setStatus(Status.FAILED);
            session.setErrorMessage("AI analysis failed: " + e.getMessage());
        }

        sessionRepo.save(session);
    }

    // ---- Feature 3: Session History ----

    @Transactional(readOnly = true)
    public Page<SessionSummary> getHistory(String email, int page, int size, String baseUrl) {
        User user = getUser(email);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return sessionRepo.findByUserOrderByCreatedAtDesc(user, pageRequest)
                .map(s -> mapper.toSummary(s, baseUrl));
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(Long sessionId, String email, String baseUrl) {
        User user = getUser(email);
        CoachingSession session = sessionRepo.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        return mapper.toResponse(session, baseUrl);
    }

    // ---- Bonus: Progress chart (score over time) ----

    @Transactional(readOnly = true)
    public List<ScoreProgressPoint> getProgress(String email) {
        User user = getUser(email);
        return sessionRepo.findScoreProgressByUser(user).stream()
                .map(row -> new ScoreProgressPoint(
                        (Long) row[0],
                        (Integer) row[1],
                        ((java.time.Instant) row[2])))
                .collect(Collectors.toList());
    }

    // ---- Helper to load authenticated user ----

    User getUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}

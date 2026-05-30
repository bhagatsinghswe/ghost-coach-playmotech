package com.playmotech.ghostcoach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.ghostcoach.dto.response.CoachingFeedback;
import com.playmotech.ghostcoach.dto.response.ScoreProgressPoint;
import com.playmotech.ghostcoach.dto.response.SessionResponse;
import com.playmotech.ghostcoach.entity.CoachingSession;
import com.playmotech.ghostcoach.entity.CoachingSession.ConfidenceLevel;
import com.playmotech.ghostcoach.entity.CoachingSession.Status;
import com.playmotech.ghostcoach.entity.User;
import com.playmotech.ghostcoach.entity.User.ExperienceLevel;
import com.playmotech.ghostcoach.entity.User.Sport;
import com.playmotech.ghostcoach.repository.CoachingSessionRepository;
import com.playmotech.ghostcoach.repository.UserRepository;
import com.playmotech.ghostcoach.service.CoachingSessionService;
import com.playmotech.ghostcoach.service.GeminiVisionService;
import com.playmotech.ghostcoach.util.FileStorageUtil;
import com.playmotech.ghostcoach.util.SessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoachingSessionServiceTest {

    private CoachingSessionRepository sessionRepo;
    private UserRepository userRepo;
    private FileStorageUtil fileStorage;
    private GeminiVisionService geminiService;
    private CoachingSessionService service;

    private User mockUser;

    @BeforeEach
    void setUp() {
        sessionRepo = mock(CoachingSessionRepository.class);
        userRepo = mock(UserRepository.class);
        fileStorage = mock(FileStorageUtil.class);
        geminiService = mock(GeminiVisionService.class);
        SessionMapper mapper = new SessionMapper(new ObjectMapper());

        service = new CoachingSessionService(
                sessionRepo, userRepo, fileStorage, geminiService, mapper, new ObjectMapper());

        mockUser = User.builder()
                .id(1L)
                .fullName("Test Player")
                .email("player@test.com")
                .sport(Sport.CRICKET)
                .position("Batsman")
                .experienceLevel(ExperienceLevel.INTERMEDIATE)
                .build();
    }

    @Test
    void triggerAnalysis_successfulAiCall_persistsFeedback() throws Exception {
        // Given
        CoachingSession session = CoachingSession.builder()
                .id(42L)
                .user(mockUser)
                .storagePath("1/image.jpg")
                .contentType("image/jpeg")
                .status(Status.PENDING)
                .build();

        when(sessionRepo.findById(42L)).thenReturn(Optional.of(session));
        when(fileStorage.readBytes("1/image.jpg")).thenReturn(new byte[]{1, 2, 3});

        CoachingFeedback mockFeedback = CoachingFeedback.builder()
                .overallScore(8)
                .strengths(List.of("Great stance", "Good grip"))
                .areasToImprove(List.of("High elbow"))
                .priorityFix("Lower your elbow on backswing")
                .drillSuggestion("Shadow batting drill 10 reps")
                .confidenceLevel(ConfidenceLevel.HIGH)
                .build();

        when(geminiService.analyzeStance(any(), eq("image/jpeg"), eq(mockUser)))
                .thenReturn(mockFeedback);

        // When
        service.triggerAnalysis(42L, "player@test.com");

        // Then
        ArgumentCaptor<CoachingSession> captor = ArgumentCaptor.forClass(CoachingSession.class);
        verify(sessionRepo, times(2)).save(captor.capture()); // PROCESSING + COMPLETED

        CoachingSession saved = captor.getAllValues().get(1);
        assertThat(saved.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(saved.getOverallScore()).isEqualTo(8);
        assertThat(saved.getPriorityFix()).isEqualTo("Lower your elbow on backswing");
        assertThat(saved.getStrengthsJson()).contains("Great stance");
        assertThat(saved.getConfidenceLevel()).isEqualTo(ConfidenceLevel.HIGH);
    }

    @Test
    void triggerAnalysis_geminiThrows_sessionMarkedFailed() throws Exception {
        // Given
        CoachingSession session = CoachingSession.builder()
                .id(99L)
                .user(mockUser)
                .storagePath("1/fail.jpg")
                .contentType("image/jpeg")
                .status(Status.PENDING)
                .build();

        when(sessionRepo.findById(99L)).thenReturn(Optional.of(session));
        when(fileStorage.readBytes(any())).thenReturn(new byte[]{1});
        when(geminiService.analyzeStance(any(), any(), any()))
                .thenThrow(new RuntimeException("Gemini timeout"));

        // When
        service.triggerAnalysis(99L, "player@test.com");

        // Then
        ArgumentCaptor<CoachingSession> captor = ArgumentCaptor.forClass(CoachingSession.class);
        verify(sessionRepo, times(2)).save(captor.capture());

        CoachingSession saved = captor.getAllValues().get(1);
        assertThat(saved.getStatus()).isEqualTo(Status.FAILED);
        assertThat(saved.getErrorMessage()).contains("Gemini timeout");
    }

    @Test
    void getProgress_mapsRawQueryResultsToDTO() {
        // Given — raw Object[] rows as returned by JPQL projection
        Instant now = Instant.now();
        List<Object[]> rows = List.of(
                new Object[]{1L, 6, now.minusSeconds(86400)},
                new Object[]{2L, 8, now}
        );
        when(userRepo.findByEmail("player@test.com")).thenReturn(Optional.of(mockUser));
        when(sessionRepo.findScoreProgressByUser(mockUser)).thenReturn(rows);

        // When
        List<ScoreProgressPoint> points = service.getProgress("player@test.com");

        // Then
        assertThat(points).hasSize(2);
        assertThat(points.get(0).getScore()).isEqualTo(6);
        assertThat(points.get(1).getScore()).isEqualTo(8);
        assertThat(points.get(1).getSessionId()).isEqualTo(2L);
    }
}

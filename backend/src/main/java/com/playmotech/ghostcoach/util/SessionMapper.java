package com.playmotech.ghostcoach.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.ghostcoach.dto.response.CoachingFeedback;
import com.playmotech.ghostcoach.dto.response.SessionResponse;
import com.playmotech.ghostcoach.dto.response.SessionSummary;
import com.playmotech.ghostcoach.entity.CoachingSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionMapper {

    private final ObjectMapper objectMapper;

    public SessionResponse toResponse(CoachingSession s, String baseUrl) {
        return SessionResponse.builder()
                .id(s.getId())
                .originalFileName(s.getOriginalFileName())
                .imageUrl(baseUrl + "/api/images/" + s.getStoragePath())
                .fileSizeBytes(s.getFileSizeBytes())
                .status(s.getStatus())
                .feedback(s.getStatus() == CoachingSession.Status.COMPLETED ? buildFeedback(s) : null)
                .errorMessage(s.getErrorMessage())
                .createdAt(s.getCreatedAt())
                .build();
    }

    public SessionSummary toSummary(CoachingSession s, String baseUrl) {
        return SessionSummary.builder()
                .id(s.getId())
                .thumbnailUrl(baseUrl + "/api/images/" + s.getStoragePath())
                .uploadedAt(s.getCreatedAt())
                .overallScore(s.getOverallScore())
                .priorityFix(s.getPriorityFix())
                .status(s.getStatus())
                .build();
    }

    private CoachingFeedback buildFeedback(CoachingSession s) {
        return CoachingFeedback.builder()
                .overallScore(s.getOverallScore())
                .strengths(parseJson(s.getStrengthsJson()))
                .areasToImprove(parseJson(s.getAreasToImproveJson()))
                .priorityFix(s.getPriorityFix())
                .drillSuggestion(s.getDrillSuggestion())
                .confidenceLevel(s.getConfidenceLevel())
                .build();
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Could not parse JSON list: {}", json);
            return Collections.emptyList();
        }
    }
}

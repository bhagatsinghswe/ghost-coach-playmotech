package com.playmotech.ghostcoach.dto.response;

import com.playmotech.ghostcoach.entity.CoachingSession.ConfidenceLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Structured AI feedback matching the assignment rubric exactly.
 */
@Data
@Builder
public class CoachingFeedback {
    private Integer overallScore;
    private List<String> strengths;
    private List<String> areasToImprove;
    private String priorityFix;
    private String drillSuggestion;
    private ConfidenceLevel confidenceLevel;
}

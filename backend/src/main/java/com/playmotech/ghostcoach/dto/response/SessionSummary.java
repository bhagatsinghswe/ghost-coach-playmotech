package com.playmotech.ghostcoach.dto.response;

import com.playmotech.ghostcoach.entity.CoachingSession.Status;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Compact representation for the history list view.
 * Contains exactly the fields shown on a session card per the spec.
 */
@Data
@Builder
public class SessionSummary {
    private Long id;
    private String thumbnailUrl;
    private Instant uploadedAt;
    private Integer overallScore;
    private String priorityFix;
    private Status status;
}

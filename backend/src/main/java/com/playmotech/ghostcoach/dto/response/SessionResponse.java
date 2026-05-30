package com.playmotech.ghostcoach.dto.response;

import com.playmotech.ghostcoach.entity.CoachingSession.Status;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SessionResponse {
    private Long id;
    private String originalFileName;
    private String imageUrl;          // Signed / served URL
    private Long fileSizeBytes;
    private Status status;
    private CoachingFeedback feedback; // null while PENDING / PROCESSING
    private String errorMessage;
    private Instant createdAt;
}

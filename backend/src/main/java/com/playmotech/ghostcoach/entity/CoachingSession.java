package com.playmotech.ghostcoach.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A coaching session represents one image upload by a player together with
 * the structured AI feedback that was returned.
 *
 * The feedback fields map directly to the rubric in the assignment spec:
 *   overallScore, strengths[], areasToImprove[], priorityFix, drillSuggestion, confidenceLevel
 */
@Entity
@Table(name = "coaching_sessions",
        indexes = {
            @Index(name = "idx_sessions_user_id", columnList = "user_id"),
            @Index(name = "idx_sessions_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoachingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ---- Image storage ----

    /** Original filename as uploaded. */
    @Column(nullable = false, length = 255)
    private String originalFileName;

    /** Stable, server-side storage path (relative to upload dir). */
    @Column(nullable = false, length = 500)
    private String storagePath;

    /** MIME type — used when serving the image back. */
    @Column(nullable = false, length = 50)
    private String contentType;

    /** File size in bytes — enforced at upload, stored for reference. */
    private Long fileSizeBytes;

    // ---- AI Feedback Fields (assignment rubric) ----

    /** 0-10 score of technique quality. Null until AI responds. */
    private Integer overallScore;

    /** 2-3 things the player is doing well — stored as JSON array string. */
    @Column(columnDefinition = "TEXT")
    private String strengthsJson;

    /** 2-3 specific flaws with plain-English explanation — JSON array string. */
    @Column(columnDefinition = "TEXT")
    private String areasToImproveJson;

    /** The single most important correction. */
    @Column(columnDefinition = "TEXT")
    private String priorityFix;

    /** One concrete drill or exercise. */
    @Column(columnDefinition = "TEXT")
    private String drillSuggestion;

    /** AI confidence in its analysis. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ConfidenceLevel confidenceLevel;

    /** Raw JSON response from Gemini — stored for debugging / re-parsing. */
    @Column(columnDefinition = "TEXT")
    private String rawAiResponse;

    /** Processing state — allows async handling. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /** Error message if AI call failed. */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // ---- Nested enums ----

    public enum ConfidenceLevel {
        LOW, MEDIUM, HIGH
    }

    public enum Status {
        PENDING,     // Image uploaded, AI call not yet made
        PROCESSING,  // AI call in-flight
        COMPLETED,   // Feedback ready
        FAILED       // AI call failed
    }
}

package com.playmotech.ghostcoach.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a registered player in Ghost Coach.
 * Profile fields (sport, position, level) are passed to the AI on every analysis
 * to personalise coaching feedback.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uq_users_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;   // BCrypt hash — never plaintext

    /** The sport the player focuses on (e.g. CRICKET, FOOTBALL, BASKETBALL, BADMINTON). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sport sport;

    /** Role / position within the sport (e.g. "Batsman", "Goalkeeper"). */
    @Column(nullable = false, length = 60)
    private String position;

    /** Skill tier — drives how the AI calibrates its feedback tone. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperienceLevel experienceLevel;

    /** Age (optional, stored for richer AI prompts). */
    private Integer age;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoachingSession> sessions = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // ---- Nested enums ----

    public enum Sport {
        CRICKET, FOOTBALL, BASKETBALL, BADMINTON
    }

    public enum ExperienceLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}

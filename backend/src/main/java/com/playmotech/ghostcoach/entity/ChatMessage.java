package com.playmotech.ghostcoach.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A single message in the AI improvement chat for a coaching session.
 *
 * The chat is context-aware — the AI is seeded with the session's feedback
 * and the player's profile before the conversation begins.
 */
@Entity
@Table(name = "chat_messages",
        indexes = @Index(name = "idx_chat_session_id", columnList = "session_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private CoachingSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public enum Role {
        USER, ASSISTANT
    }
}

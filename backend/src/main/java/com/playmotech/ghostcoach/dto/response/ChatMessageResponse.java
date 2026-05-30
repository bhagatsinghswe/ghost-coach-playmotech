package com.playmotech.ghostcoach.dto.response;

import com.playmotech.ghostcoach.entity.ChatMessage.Role;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private Role role;
    private String content;
    private Instant createdAt;
}

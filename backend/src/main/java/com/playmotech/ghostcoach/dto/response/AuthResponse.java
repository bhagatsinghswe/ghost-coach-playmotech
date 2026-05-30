package com.playmotech.ghostcoach.dto.response;

import com.playmotech.ghostcoach.entity.User.ExperienceLevel;
import com.playmotech.ghostcoach.entity.User.Sport;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String fullName;
    private String email;
    private Sport sport;
    private String position;
    private ExperienceLevel experienceLevel;
    private Integer age;
}

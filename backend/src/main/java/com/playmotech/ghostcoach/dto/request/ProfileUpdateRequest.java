package com.playmotech.ghostcoach.dto.request;

import com.playmotech.ghostcoach.entity.User.ExperienceLevel;
import com.playmotech.ghostcoach.entity.User.Sport;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Used for PATCH /api/profile — all fields optional so players can update individually.
 */
@Data
public class ProfileUpdateRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    private Sport sport;

    @Size(max = 60, message = "Position must be at most 60 characters")
    private String position;

    private ExperienceLevel experienceLevel;

    @Min(value = 5, message = "Age must be at least 5")
    @Max(value = 100, message = "Age must be at most 100")
    private Integer age;
}

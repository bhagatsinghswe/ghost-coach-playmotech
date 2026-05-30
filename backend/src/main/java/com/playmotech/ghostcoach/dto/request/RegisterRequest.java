package com.playmotech.ghostcoach.dto.request;

import com.playmotech.ghostcoach.entity.User.ExperienceLevel;
import com.playmotech.ghostcoach.entity.User.Sport;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Sport is required")
    private Sport sport;

    @NotBlank(message = "Position / role is required")
    @Size(max = 60, message = "Position must be at most 60 characters")
    private String position;

    @NotNull(message = "Experience level is required")
    private ExperienceLevel experienceLevel;

    @Min(value = 5, message = "Age must be at least 5")
    @Max(value = 100, message = "Age must be at most 100")
    private Integer age;
}

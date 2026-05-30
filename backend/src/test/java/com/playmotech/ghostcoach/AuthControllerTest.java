package com.playmotech.ghostcoach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.ghostcoach.dto.request.LoginRequest;
import com.playmotech.ghostcoach.dto.request.RegisterRequest;
import com.playmotech.ghostcoach.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void registerAndLogin_happyPath() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Virat Kohli Test");
        reg.setEmail("virat.test@example.com");
        reg.setPassword("Password123!");
        reg.setSport(User.Sport.CRICKET);
        reg.setPosition("Batsman");
        reg.setExperienceLevel(User.ExperienceLevel.ADVANCED);
        reg.setAge(35);

        // Register
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/token").asText();
        assertThat(token).isNotBlank();

        // Login
        LoginRequest login = new LoginRequest();
        login.setEmail("virat.test@example.com");
        login.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.sport").value("CRICKET"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Duplicate User");
        reg.setEmail("duplicate@example.com");
        reg.setPassword("Password123!");
        reg.setSport(User.Sport.FOOTBALL);
        reg.setPosition("Striker");
        reg.setExperienceLevel(User.ExperienceLevel.BEGINNER);

        // First registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Second registration with same email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setEmail("nonexistent@example.com");
        login.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Bad Email User");
        reg.setEmail("not-an-email");
        reg.setPassword("Password123!");
        reg.setSport(User.Sport.BASKETBALL);
        reg.setPosition("Point Guard");
        reg.setExperienceLevel(User.ExperienceLevel.INTERMEDIATE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }
}

package com.playmotech.ghostcoach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.ghostcoach.config.AppProperties;
import com.playmotech.ghostcoach.dto.response.CoachingFeedback;
import com.playmotech.ghostcoach.entity.CoachingSession.ConfidenceLevel;
import com.playmotech.ghostcoach.entity.User;
import com.playmotech.ghostcoach.service.GeminiVisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GeminiVisionServiceTest {

    private GeminiVisionService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getGemini().setApiKey("test-key");
        props.getGemini().setModel("gemini-1.5-flash");
        service = new GeminiVisionService(mock(WebClient.class), props, new ObjectMapper());
    }

    @Test
    void parseAnalysisResponse_validJson_returnsFeedback() throws Exception {
        String geminiResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"overallScore\\": 7, \\"strengths\\": [\\"Good grip\\", \\"Balanced stance\\"], \\"areasToImprove\\": [\\"Elbow too high\\"], \\"priorityFix\\": \\"Lower your elbow on backswing\\", \\"drillSuggestion\\": \\"Shadow batting drill\\", \\"confidenceLevel\\": \\"HIGH\\"}"
                  }]
                }
              }]
            }
            """;

        Method m = GeminiVisionService.class.getDeclaredMethod("parseAnalysisResponse", String.class);
        m.setAccessible(true);
        CoachingFeedback feedback = (CoachingFeedback) m.invoke(service, geminiResponse);

        assertThat(feedback.getOverallScore()).isEqualTo(7);
        assertThat(feedback.getStrengths()).containsExactly("Good grip", "Balanced stance");
        assertThat(feedback.getAreasToImprove()).containsExactly("Elbow too high");
        assertThat(feedback.getPriorityFix()).isEqualTo("Lower your elbow on backswing");
        assertThat(feedback.getDrillSuggestion()).isEqualTo("Shadow batting drill");
        assertThat(feedback.getConfidenceLevel()).isEqualTo(ConfidenceLevel.HIGH);
    }

    @Test
    void parseAnalysisResponse_withMarkdownFences_stripsAndParses() throws Exception {
        String geminiResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "```json\\n{\\"overallScore\\": 5, \\"strengths\\": [\\"Good footwork\\"], \\"areasToImprove\\": [\\"Watch the ball\\"], \\"priorityFix\\": \\"Keep eyes on ball\\", \\"drillSuggestion\\": \\"Wall ball drill\\", \\"confidenceLevel\\": \\"MEDIUM\\"}\\n```"
                  }]
                }
              }]
            }
            """;

        Method m = GeminiVisionService.class.getDeclaredMethod("parseAnalysisResponse", String.class);
        m.setAccessible(true);
        CoachingFeedback feedback = (CoachingFeedback) m.invoke(service, geminiResponse);

        assertThat(feedback.getOverallScore()).isEqualTo(5);
        assertThat(feedback.getConfidenceLevel()).isEqualTo(ConfidenceLevel.MEDIUM);
    }

    @Test
    void sportSpecificCues_allSportsHaveCues() throws Exception {
        Method m = GeminiVisionService.class.getDeclaredMethod("sportSpecificCues", User.Sport.class);
        m.setAccessible(true);

        for (User.Sport sport : User.Sport.values()) {
            String cues = (String) m.invoke(service, sport);
            assertThat(cues).isNotBlank()
                    .withFailMessage("Missing cues for sport: " + sport);
        }
    }
}

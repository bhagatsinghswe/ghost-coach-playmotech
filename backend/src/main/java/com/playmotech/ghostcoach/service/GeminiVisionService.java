package com.playmotech.ghostcoach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.ghostcoach.config.AppProperties;
import com.playmotech.ghostcoach.dto.response.CoachingFeedback;
import com.playmotech.ghostcoach.entity.CoachingSession.ConfidenceLevel;
import com.playmotech.ghostcoach.entity.User;
import com.playmotech.ghostcoach.exception.GeminiApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

/**
 * Wraps the Gemini Vision (gemini-1.5-flash) API.
 *
 * Prompt design rationale:
 *  - System context establishes the "Ghost Coach" persona so the model stays on-task.
 *  - Player profile (sport, position, level, age) is injected upfront so every field of
 *    the rubric is personalised — a beginner batsman gets simpler language than an
 *    advanced one.
 *  - The model is told to respond ONLY in a specific JSON schema matching the rubric.
 *    This makes parsing deterministic and failure modes obvious.
 *  - Sport-specific cues (e.g., "grip, stance, backlift" for cricket) are inserted
 *    dynamically to guide the visual analysis toward domain-relevant features.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiVisionService {

    private final WebClient geminiWebClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    // ---- Analysis entry point ----

    public CoachingFeedback analyzeStance(byte[] imageBytes, String mimeType, User player) {
        String prompt = buildAnalysisPrompt(player);
        String responseJson = callGemini(imageBytes, mimeType, prompt);
        return parseAnalysisResponse(responseJson);
    }

    // ---- Chat entry point (context-aware coaching chat) ----

    public String chat(String userMessage, CoachingFeedback sessionFeedback, User player,
                       List<Map<String, String>> history) {
        String systemContext = buildChatSystemContext(player, sessionFeedback);
        List<Map<String, Object>> contents = buildChatContents(systemContext, history, userMessage);
        String raw = callGeminiText(contents);
        return extractTextFromResponse(raw);
    }

    // ---- Prompt construction ----

    private String buildAnalysisPrompt(User player) {
        String sportCues = sportSpecificCues(player.getSport());
        String levelTone = levelTone(player.getExperienceLevel());
        String ageContext = player.getAge() != null
                ? "The player is " + player.getAge() + " years old. " : "";

        return """
            You are Ghost Coach — an expert AI sports coach specialising in %s.
            
            PLAYER PROFILE:
            - Name: %s
            - Sport: %s
            - Position / Role: %s
            - Experience Level: %s
            - %s
            
            COACHING BRIEF:
            %s
            %s
            
            TASK:
            Analyse the technique visible in the attached image. Focus specifically on: %s
            
            RESPONSE FORMAT — reply with ONLY a valid JSON object, no markdown, no preamble:
            {
              "overallScore": <integer 1-10>,
              "strengths": ["<strength 1>", "<strength 2>"],
              "areasToImprove": ["<area 1>", "<area 2>"],
              "priorityFix": "<the single most important correction>",
              "drillSuggestion": "<one concrete drill or exercise>",
              "confidenceLevel": "<LOW|MEDIUM|HIGH>"
            }
            
            SCORING GUIDE (1-10):
            1-3: Significant technique issues needing immediate correction.
            4-6: Developing technique with clear areas for improvement.
            7-8: Good technique with minor refinements needed.
            9-10: Excellent, near-professional technique.
            
            CONFIDENCE GUIDE:
            HIGH   — clear, well-lit photo showing the relevant technique fully.
            MEDIUM — partially visible or slightly blurry but enough to assess.
            LOW    — poor lighting, wrong angle, or technique not clearly visible.
            
            Provide 2-3 items each for strengths and areasToImprove.
            Keep language %s. Be specific and actionable, not generic.
            """.formatted(
                player.getSport().name().toLowerCase(),
                player.getFullName(),
                player.getSport().name(),
                player.getPosition(),
                player.getExperienceLevel().name().toLowerCase(),
                ageContext,
                levelTone,
                ageContext.isBlank() ? "" : "Tailor complexity of language and drills to their age.",
                sportCues,
                levelTone(player.getExperienceLevel())
        );
    }

    private String buildChatSystemContext(User player, CoachingFeedback feedback) {
        return """
            You are Ghost Coach, a personalised AI sports coach for %s.
            
            PLAYER: %s | Sport: %s | Role: %s | Level: %s
            
            SESSION FEEDBACK CONTEXT:
            - Overall Score: %d/10
            - Strengths: %s
            - Areas to Improve: %s
            - Priority Fix: %s
            - Drill Suggested: %s
            
            You are having a follow-up coaching conversation about this session.
            Stay in character as a knowledgeable, encouraging coach.
            Reference the session feedback when relevant.
            Keep answers concise (2-4 sentences) unless a longer explanation is needed.
            """.formatted(
                player.getSport().name().toLowerCase(),
                player.getFullName(),
                player.getSport().name(),
                player.getPosition(),
                player.getExperienceLevel().name().toLowerCase(),
                feedback != null && feedback.getOverallScore() != null ? feedback.getOverallScore() : 0,
                feedback != null ? String.join("; ", feedback.getStrengths()) : "N/A",
                feedback != null ? String.join("; ", feedback.getAreasToImprove()) : "N/A",
                feedback != null ? feedback.getPriorityFix() : "N/A",
                feedback != null ? feedback.getDrillSuggestion() : "N/A"
        );
    }

    private String sportSpecificCues(User.Sport sport) {
        return switch (sport) {
            case CRICKET -> "batting grip, stance width, bat lift, head position, weight transfer, elbow alignment, and follow-through";
            case FOOTBALL -> "body posture, foot placement, knee bend, balance, and body orientation relative to the ball";
            case BASKETBALL -> "shooting form (BEEF: Balance, Eyes, Elbow, Follow-through), dribble posture, footwork, and hand placement";
            case BADMINTON -> "grip type, racket position, footwork stance, shoulder rotation, wrist angle, and ready position";
        };
    }

    private String levelTone(User.ExperienceLevel level) {
        return switch (level) {
            case BEGINNER -> "simple, encouraging, and jargon-free — use plain English a new player can immediately act on";
            case INTERMEDIATE -> "technically specific with proper terminology — assume basic sport knowledge";
            case ADVANCED -> "detailed and precise — use advanced coaching terminology, reference biomechanics where relevant";
        };
    }

    // ---- Gemini API calls ----

    private String callGemini(byte[] imageBytes, String mimeType, String prompt) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> inlinePart = Map.of(
                "inlineData", Map.of("mimeType", mimeType, "data", base64Image));
        Map<String, Object> textPart = Map.of("text", prompt);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(inlinePart, textPart))),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "topP", 0.8,
                        "maxOutputTokens", 1024
                )
        );

        return executeRequest(body);
    }

    private String callGeminiText(List<Map<String, Object>> contents) {
        Map<String, Object> body = Map.of(
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 512
                )
        );
        return executeRequest(body);
    }

    private String executeRequest(Map<String, Object> body) {
        String model = appProperties.getGemini().getModel();
        String apiKey = appProperties.getGemini().getApiKey();
        String uri = "/models/" + model + ":generateContent?key=" + apiKey;

        try {
            return geminiWebClient.post()
                    .uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Gemini API HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeminiApiException("Gemini API returned " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            throw new GeminiApiException("Gemini API call failed: " + e.getMessage(), e);
        }
    }

    // ---- Response parsing ----

    private CoachingFeedback parseAnalysisResponse(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String text = root.at("/candidates/0/content/parts/0/text").asText();

            // Strip markdown code fences if Gemini wraps output despite instructions
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            JsonNode feedback = objectMapper.readTree(text);

            List<String> strengths = new ArrayList<>();
            feedback.path("strengths").forEach(n -> strengths.add(n.asText()));

            List<String> areas = new ArrayList<>();
            feedback.path("areasToImprove").forEach(n -> areas.add(n.asText()));

            ConfidenceLevel confidence = parseConfidence(
                    feedback.path("confidenceLevel").asText("MEDIUM"));

            return CoachingFeedback.builder()
                    .overallScore(feedback.path("overallScore").asInt(5))
                    .strengths(strengths)
                    .areasToImprove(areas)
                    .priorityFix(feedback.path("priorityFix").asText())
                    .drillSuggestion(feedback.path("drillSuggestion").asText())
                    .confidenceLevel(confidence)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", rawJson, e);
            throw new GeminiApiException("Could not parse AI response: " + e.getMessage(), e);
        }
    }

    private String extractTextFromResponse(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            return root.at("/candidates/0/content/parts/0/text").asText("Sorry, I could not generate a response.");
        } catch (Exception e) {
            log.warn("Could not extract chat text from Gemini response", e);
            return "Sorry, I could not generate a response at this time.";
        }
    }

    private ConfidenceLevel parseConfidence(String raw) {
        try {
            return ConfidenceLevel.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ConfidenceLevel.MEDIUM;
        }
    }

    // ---- Chat history builder ----

    private List<Map<String, Object>> buildChatContents(String systemContext,
                                                         List<Map<String, String>> history,
                                                         String userMessage) {
        List<Map<String, Object>> contents = new ArrayList<>();

        // Gemini uses "user/model" roles. Inject system context as first user turn.
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", systemContext))));
        contents.add(Map.of(
                "role", "model",
                "parts", List.of(Map.of("text", "Understood. I'm ready to help as Ghost Coach."))));

        // Replay conversation history
        for (Map<String, String> msg : history) {
            String role = "USER".equals(msg.get("role")) ? "user" : "model";
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", msg.get("content")))));
        }

        // New user message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))));

        return contents;
    }
}

package com.opsflow.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Asks a language model for the recommendation, through the chat-completions API that OpenAI,
 * Ollama and most hosted models share. Switched on with opsflow.ai.provider=openai.
 *
 * The model is given the evidence the backend collected and the list of actions it may choose
 * from. It gets no tools and no database access of its own. Its answer is treated as untrusted
 * input: it is parsed, the action is checked against ActionPolicy by the caller, and if the
 * call fails or the answer can't be read, the rule-based provider answers in its place.
 */
@Component
@Primary
@ConditionalOnProperty(name = "opsflow.ai.provider", havingValue = "openai")
public class OpenAiCompatibleProvider implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);

    private final RestClient http;
    private final String model;
    private final ObjectMapper objectMapper;
    private final ActionPolicy policy;
    private final RuleBasedAIProvider fallback;

    public OpenAiCompatibleProvider(RestClient.Builder builder,
                                    @Value("${opsflow.ai.base-url:https://api.openai.com/v1}") String baseUrl,
                                    @Value("${opsflow.ai.api-key:}") String apiKey,
                                    @Value("${opsflow.ai.model:gpt-4o-mini}") String model,
                                    ObjectMapper objectMapper,
                                    ActionPolicy policy,
                                    RuleBasedAIProvider fallback) {
        this.http = builder.baseUrl(baseUrl).defaultHeader("Authorization", "Bearer " + apiKey).build();
        this.model = model;
        this.objectMapper = objectMapper;
        this.policy = policy;
        this.fallback = fallback;
    }

    @Override
    public String name() {
        return "model:" + model;
    }

    @Override
    public ClassificationResult classify(String title, String description) {
        return fallback.classify(title, description);   // keywords are enough for routing; the model is kept for the judgement call
    }

    @Override
    public Recommendation recommend(Evidence evidence) {
        try {
            JsonNode answer = objectMapper.readTree(ask(evidence));
            double confidence = Math.max(0, Math.min(1, answer.path("confidence").asDouble(0.5)));
            return Recommendation.builder()
                    .summary(answer.path("summary").asText("The model gave no summary."))
                    .likelyCause(answer.path("likelyCause").asText("The model gave no cause."))
                    .recommendedAction(answer.path("recommendedAction").asText(ActionPolicy.NEEDS_HUMAN_REVIEW))
                    .confidence(confidence)
                    .build();
        } catch (Exception e) {
            logger.warn("The model could not be used ({}); falling back to the rules", e.getMessage());
            return fallback.recommend(evidence);
        }
    }

    private String ask(Evidence evidence) {
        String system = "You help an operations team decide what to do about a customer request. "
                + "Use only the evidence given. Answer with one JSON object with the fields summary, likelyCause, "
                + "recommendedAction and confidence (0 to 1). recommendedAction must be exactly one of: "
                + String.join(", ", policy.allActions()) + ". If the evidence is not enough, choose "
                + ActionPolicy.REQUEST_MORE_INFORMATION + ".";
        String user = "Title: " + evidence.request().getTitle() + "\n"
                + "Description: " + evidence.request().getDescription() + "\n"
                + "Evidence:\n- " + String.join("\n- ", evidence.asLines());

        JsonNode response = http.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "temperature", 0,
                        "response_format", Map.of("type", "json_object"),
                        "messages", List.of(
                                Map.of("role", "system", "content", system),
                                Map.of("role", "user", "content", user))))
                .retrieve()
                .body(JsonNode.class);
        return response.path("choices").path(0).path("message").path("content").asText();
    }
}

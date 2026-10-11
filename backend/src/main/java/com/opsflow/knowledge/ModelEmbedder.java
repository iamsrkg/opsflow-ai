package com.opsflow.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Embeddings from a language model, through the OpenAI-compatible "/embeddings" call that
 * OpenAI, Ollama and most model servers offer. Unlike feature hashing, a model places texts
 * with the same meaning close together even when they share no words.
 *
 * Switched on with EMBEDDING_PROVIDER=model. docker-compose.ai.yml runs it against a local
 * Ollama, so no API key is needed.
 */
@Component
@Primary
@ConditionalOnProperty(name = "opsflow.embedding.provider", havingValue = "model")
public class ModelEmbedder implements TextEmbedder {

    private final RestClient http;
    private final String model;
    private final int dimensions;
    private final double minimumScore;

    public ModelEmbedder(RestClient.Builder builder,
                         @Value("${opsflow.embedding.base-url:${opsflow.ai.base-url:https://api.openai.com/v1}}") String baseUrl,
                         @Value("${opsflow.embedding.api-key:${opsflow.ai.api-key:}}") String apiKey,
                         @Value("${opsflow.embedding.model:all-minilm}") String model,
                         @Value("${opsflow.embedding.dimensions:384}") int dimensions,
                         @Value("${opsflow.embedding.minimum-score:0.3}") double minimumScore) {
        this.http = builder.baseUrl(baseUrl).defaultHeader("Authorization", "Bearer " + apiKey).build();
        this.model = model;
        this.dimensions = dimensions;
        this.minimumScore = minimumScore;
    }

    @Override
    public String name() {
        return "model:" + model;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public double minimumScore() {
        return minimumScore;
    }

    @Override
    public float[] embed(String text) {
        JsonNode response = http.post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "input", text == null ? "" : text))
                .retrieve()
                .body(JsonNode.class);
        JsonNode values = response == null ? null : response.path("data").path(0).path("embedding");
        if (values == null || !values.isArray() || values.size() != dimensions) {
            throw new IllegalStateException("The embedding model " + model + " returned "
                    + (values == null || !values.isArray() ? "no vector" : values.size() + " dimensions")
                    + ", expected " + dimensions);
        }
        float[] vector = new float[dimensions];
        double sum = 0;
        for (int i = 0; i < dimensions; i++) {
            vector[i] = (float) values.get(i).asDouble();
            sum += vector[i] * vector[i];
        }
        if (sum > 0) {
            float length = (float) Math.sqrt(sum);
            for (int i = 0; i < dimensions; i++) {
                vector[i] /= length;   // length 1, so cosine similarity is a plain dot product
            }
        }
        return vector;
    }
}

package com.vpms.mcp.qdrant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpms.mcp.qdrant.config.EmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class EmbeddingServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceClient.class);
    
    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;
    
    public EmbeddingServiceClient(EmbeddingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    private WebClient webClient() {
        return WebClient.builder()
                .baseUrl(properties.getServiceUrl())
                .build();
    }

    public float[] embed(String text) {
        try {
            return webClient().post()
                    .uri("/api/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequestBody(text))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            Mono.error(new RuntimeException("Embedding service error: " + response.statusCode())))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                    .map(this::parseEmbedding)
                    .block();
        } catch (Exception e) {
            log.error("Embedding request failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += properties.getBatchSize()) {
            List<String> batch = texts.subList(i, Math.min(i + properties.getBatchSize(), texts.size()));

            for (String text : batch) {
                try {
                    results.add(embed(text));
                } catch (Exception e) {
                    log.error("Failed to embed text batch item: {}", e.getMessage());
                    results.add(new float[properties.getDimension()]);
                }
            }
        }

        return results;
    }

    private String createRequestBody(String text) {
        try {
            return objectMapper.writeValueAsString(new EmbeddingRequest(properties.getModel(), text));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create request body", e);
        }
    }

    private float[] parseEmbedding(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode embedding = root.get("embedding");

            if (embedding == null || !embedding.isArray()) {
                throw new RuntimeException("Invalid embedding response format");
            }

            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = embedding.get(i).floatValue();
            }
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse embedding response", e);
        }
    }

    private record EmbeddingRequest(String model, String prompt) {}
}

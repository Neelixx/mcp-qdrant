package com.vpms.mcp.qdrant.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpms.mcp.qdrant.config.EmbeddingProperties;

import reactor.core.publisher.Mono;

@Component
public class EmbeddingServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceClient.class);
    
// changed: Remove silent data corruption + WebClient reuse
private final EmbeddingProperties properties;
private final ObjectMapper objectMapper;
private final WebClient webClient;
 
public EmbeddingServiceClient(EmbeddingProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.webClient = WebClient.builder()
            .baseUrl(properties.getServiceUrl())
            .build();
}

    public float[] embed(String text) {
        try {
            return webClient.post()
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
    if (texts.isEmpty()) {
        return new ArrayList<>();
    }
    
    // Send actual batch request to embedding service
    try {
        return webClient.post()
                .uri("/api/embeddings/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createBatchRequestBody(texts))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new RuntimeException("Embedding service error: " + response.statusCode())))
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .map(this::parseBatchEmbedding)
                .block();
    } catch (Exception e) {
        log.error("Batch embedding request failed: {}", e.getMessage());
        throw new RuntimeException("Failed to generate batch embeddings: " + e.getMessage(), e);
    }
}

    private String createBatchRequestBody(List<String> texts) {
        try {
            List<EmbeddingRequest> requests = texts.stream()
                    .map(text -> new EmbeddingRequest(properties.getModel(), text))
                    .toList();
            return objectMapper.writeValueAsString(requests);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create batch request body", e);
        }
    }

    private List<float[]> parseBatchEmbedding(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            if (!root.isArray()) {
                throw new RuntimeException("Invalid batch embedding response format - expected array");
            }
            
            List<float[]> results = new ArrayList<>();
            for (JsonNode item : root) {
                JsonNode embedding = item.get("embedding");
                if (embedding == null || !embedding.isArray()) {
                    throw new RuntimeException("Invalid embedding item in batch response");
                }
                float[] result = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    result[i] = embedding.get(i).floatValue();
                }
                results.add(result);
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse batch embedding response", e);
        }
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

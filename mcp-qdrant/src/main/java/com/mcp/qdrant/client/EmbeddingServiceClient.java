package com.mcp.qdrant.client;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.qdrant.config.EmbeddingProperties;

@Component
public class EmbeddingServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceClient.class);
    
    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public EmbeddingServiceClient(EmbeddingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public float[] embed(String text) {
        return embedWithModel(text, properties.getModel());
    }
    
    public float[] embedWithModel(String text, String modelName) {
        try {
            String url = properties.getServiceUrl() + "/api/embeddings";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = createRequestBody(text, modelName);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Embedding service error: " + response.getStatusCode());
            }

            return parseEmbedding(response.getBody());
        } catch (Exception e) {
            log.error("Embedding request failed for model '{}': {}", modelName, e.getMessage());
            throw new RuntimeException("Failed to generate embedding with model " + modelName + ": " + e.getMessage(), e);
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        return embedBatchWithModel(texts, properties.getModel());
    }
    
    public List<float[]> embedBatchWithModel(List<String> texts, String modelName) {
        if (texts.isEmpty()) {
            return new ArrayList<>();
        }

        // Ollama doesn't have a batch endpoint, so we call single embeddings
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embedWithModel(text, modelName));
        }
        return results;
    }

    private String createRequestBody(String text) {
        return createRequestBody(text, properties.getModel());
    }

    private String createRequestBody(String text, String modelName) {
        try {
            return objectMapper.writeValueAsString(new EmbeddingRequest(modelName, text));
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

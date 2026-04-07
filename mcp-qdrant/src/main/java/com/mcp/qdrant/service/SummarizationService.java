package com.mcp.qdrant.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.qdrant.config.EmbeddingProperties;
import com.mcp.qdrant.config.QdrantProperties;
import com.mcp.qdrant.model.SearchResult;

@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);
    
    private final QdrantProperties qdrantProperties;
    private final EmbeddingProperties embeddingProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    public SummarizationService(QdrantProperties qdrantProperties, EmbeddingProperties embeddingProperties, ObjectMapper objectMapper) {
        this.qdrantProperties = qdrantProperties;
        this.embeddingProperties = embeddingProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public String summarize(String query, List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        String summarizeModel = qdrantProperties.getSummarizeModel();
        
        // If LLM model is configured, try to use it
        if (summarizeModel != null && !summarizeModel.isEmpty()) {
            try {
                return generateLLMSummary(query, results, summarizeModel);
            } catch (Exception e) {
                log.warn("LLM summarization failed, falling back to simple summary: {}", e.getMessage());
                // Fall through to simple summary
            }
        }
        
        // Simple concatenation fallback
        return generateSimpleSummary(query, results);
    }
    
    private String generateLLMSummary(String query, List<SearchResult> results, String model) {
        StringBuilder context = new StringBuilder();
        context.append("Based on the following search results, provide a concise summary answering the query.\n\n");
        context.append("Query: ").append(query).append("\n\n");
        context.append("Search Results:\n");
        
        int maxResults = qdrantProperties.getSummaryMaxResults();
        int maxLength = qdrantProperties.getSummaryMaxLength();
        
        for (int i = 0; i < Math.min(results.size(), maxResults); i++) {
            SearchResult result = results.get(i);
            String content = truncate(result.getPayload(), maxLength);
            context.append("[").append(i + 1).append("] ")
                   .append("Source: ").append(result.getCollection())
                   .append(" (Score: ").append(String.format("%.3f", result.getScore())).append(")\n")
                   .append(content)
                   .append("\n\n");
        }
        
        context.append("\nProvide a brief summary of the key information relevant to the query.");
        
        String prompt = context.toString();
        
        try {
            String url = embeddingProperties.getServiceUrl() + "/api/generate";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String requestBody = objectMapper.writeValueAsString(new GenerateRequest(model, prompt, false));
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("LLM service error: " + response.getStatusCode());
            }
            
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode responseNode = root.get("response");
            
            if (responseNode != null && !responseNode.isNull()) {
                return responseNode.asText();
            }
            
            throw new RuntimeException("No response in LLM output");
            
        } catch (Exception e) {
            log.error("LLM generation failed: {}", e.getMessage());
            throw new RuntimeException("LLM summarization failed", e);
        }
    }

    private String generateSimpleSummary(String query, List<SearchResult> results) {
        StringBuilder summary = new StringBuilder();
        int maxResults = qdrantProperties.getSummaryMaxResults();
        int maxLength = qdrantProperties.getSummaryMaxLength();

        summary.append("Based on the search results from ")
               .append(results.size())
               .append(" sources:\n\n");

        List<String> keyPoints = results.stream()
                .limit(maxResults)
                .map(r -> "- " + truncate(r.getPayload(), maxLength))
                .collect(Collectors.toList());

        summary.append(String.join("\n", keyPoints));

        if (results.size() > maxResults) {
            summary.append("\n\n... and ").append(results.size() - maxResults).append(" more results.");
        }

        return summary.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    private record GenerateRequest(String model, String prompt, boolean stream) {}
}

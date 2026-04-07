package com.mcp.qdrant.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mcp.embedding")
public class EmbeddingProperties {
    private String serviceUrl = "http://localhost:11434";
    private String model = "nomic-embed-text-v2-moe";
    private int dimension = 768;
    private int timeoutMs = 5000;
    private int batchSize = 32;
    private Map<String, CollectionEmbeddingConfig> collectionModels = new HashMap<>();
    
    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getDimension() { return dimension; }
    public void setDimension(int dimension) { this.dimension = dimension; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public Map<String, CollectionEmbeddingConfig> getCollectionModels() { return collectionModels; }
    public void setCollectionModels(Map<String, CollectionEmbeddingConfig> collectionModels) { this.collectionModels = collectionModels; }
    
    public CollectionEmbeddingConfig getConfigForCollection(String collectionName) {
        return collectionModels.get(collectionName);
    }
    
    public static class CollectionEmbeddingConfig {
        private String model;
        private int dimension;
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
    }
}

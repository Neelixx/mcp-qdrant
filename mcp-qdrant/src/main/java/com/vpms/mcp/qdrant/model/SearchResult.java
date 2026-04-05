package com.vpms.mcp.qdrant.model;

import java.util.Map;

public class SearchResult {
    private String id;
    private float score;
    private String payload;
    private String collection;
    private Map<String, String> metadata;
    
    public SearchResult() {}
    
    private SearchResult(Builder builder) {
        this.id = builder.id;
        this.score = builder.score;
        this.payload = builder.payload;
        this.collection = builder.collection;
        this.metadata = builder.metadata;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private String id;
        private float score;
        private String payload;
        private String collection;
        private Map<String, String> metadata;
        
        public Builder id(String id) { this.id = id; return this; }
        public Builder score(float score) { this.score = score; return this; }
        public Builder payload(String payload) { this.payload = payload; return this; }
        public Builder collection(String collection) { this.collection = collection; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public SearchResult build() { return new SearchResult(this); }
    }
}

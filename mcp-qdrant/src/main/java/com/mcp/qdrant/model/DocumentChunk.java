package com.mcp.qdrant.model;

import java.util.Map;

public class DocumentChunk {
    private String chunkId;
    private String content;
    private Map<String, String> metadata;
    private int sequenceNumber;
    
    public DocumentChunk() {}
    
    private DocumentChunk(Builder builder) {
        this.chunkId = builder.chunkId;
        this.content = builder.content;
        this.metadata = builder.metadata;
        this.sequenceNumber = builder.sequenceNumber;
    }
    
    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private String chunkId;
        private String content;
        private Map<String, String> metadata;
        private int sequenceNumber;
        
        public Builder chunkId(String chunkId) { this.chunkId = chunkId; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public Builder sequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; return this; }
        public DocumentChunk build() { return new DocumentChunk(this); }
    }
}

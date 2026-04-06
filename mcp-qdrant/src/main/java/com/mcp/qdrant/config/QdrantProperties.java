package com.mcp.qdrant.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mcp.qdrant")
public class QdrantProperties {
    private String host = "localhost";
    private int port = 6334;
    private int httpPort = 6333;
    private boolean useTls = false;
    private String apiKey;
    private int timeoutMs = 10000;
    private List<String> collections = List.of("vpms", "vpmshelp");
    private int searchLimit = 10;
    private float searchThreshold = 0.7f;
    
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getHttpPort() { return httpPort; }
    public void setHttpPort(int httpPort) { this.httpPort = httpPort; }
    public boolean isUseTls() { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public List<String> getCollections() { return collections; }
    public void setCollections(List<String> collections) { this.collections = collections; }
    
    public boolean isAllCollections() {
        if (collections == null || collections.isEmpty()) {
            return true;
        }
        return collections.size() == 1 && ("all".equalsIgnoreCase(collections.get(0)) || collections.get(0).isEmpty());
    }
    public int getSearchLimit() { return searchLimit; }
    public void setSearchLimit(int searchLimit) { this.searchLimit = searchLimit; }
    public float getSearchThreshold() { return searchThreshold; }
    public void setSearchThreshold(float searchThreshold) { this.searchThreshold = searchThreshold; }
}

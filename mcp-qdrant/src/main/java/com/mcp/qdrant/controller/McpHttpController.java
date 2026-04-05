package com.mcp.qdrant.controller;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.qdrant.proto.CreateCollectionRequest;
import com.mcp.qdrant.proto.CreateCollectionResponse;
import com.mcp.qdrant.proto.DeleteCollectionRequest;
import com.mcp.qdrant.proto.DeleteCollectionResponse;
import com.mcp.qdrant.proto.GetCollectionInfoRequest;
import com.mcp.qdrant.proto.GetCollectionInfoResponse;
import com.mcp.qdrant.proto.HybridSearchRequest;
import com.mcp.qdrant.proto.HybridSearchResponse;
import com.mcp.qdrant.proto.IngestDocumentRequest;
import com.mcp.qdrant.proto.IngestDocumentResponse;
import com.mcp.qdrant.proto.ListCollectionsRequest;
import com.mcp.qdrant.proto.ListCollectionsResponse;
import com.mcp.qdrant.proto.McpQdrantServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@RestController
@RequestMapping("/mcp")
public class McpHttpController {

    private static final Logger log = LoggerFactory.getLogger(McpHttpController.class);

    private final ObjectMapper objectMapper;
    private ManagedChannel channel;
    private McpQdrantServiceGrpc.McpQdrantServiceBlockingStub stub;

    public McpHttpController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder.forAddress("localhost", 9091)
                .usePlaintext()
                .build();
        stub = McpQdrantServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("gRPC channel did not terminate in 5 seconds, forcing shutdown");
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for channel shutdown", e);
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleMcpRequest(@RequestBody Map<String, Object> request) {
        String method = (String) request.get("method");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        Object id = request.get("id");

        log.info("MCP request: method={}, id={}", method, id);

        // Validate required fields
        if (method == null || method.isEmpty()) {
            log.error("MCP request missing required 'method' field");
            return ResponseEntity.ok(Map.of(
                    "jsonrpc", "2.0",
                    "id", id != null ? id : "null",
                    "error", Map.of(
                            "code", -32600,
                            "message", "Invalid Request: missing required 'method' field"
                    )
            ));
        }

        try {
            Object result = dispatchMethod(method, params);
            return ResponseEntity.ok(Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", result
            ));
        } catch (Exception e) {
            log.error("MCP error: {}", e.getMessage(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            return ResponseEntity.ok(Map.of(
                    "jsonrpc", "2.0",
                    "id", id != null ? id : "null",
                    "error", Map.of(
                            "code", -32603,
                            "message", errorMessage
                    )
            ));
        }
    }

    private Object dispatchMethod(String method, Map<String, Object> params) {
        params = params != null ? params : java.util.Collections.emptyMap();
        return switch (method) {
            case "listCollections" -> handleListCollections();
            case "getCollectionInfo" -> handleGetCollectionInfo(params);
            case "hybridSearch" -> handleHybridSearch(params);
            case "ingestDocument" -> handleIngestDocument(params);
            case "createCollection" -> handleCreateCollection(params);
            case "deleteCollection" -> handleDeleteCollection(params);
            default -> throw new RuntimeException("Unknown method: " + method);
        };
    }

    private Object handleListCollections() {
        ListCollectionsRequest request = ListCollectionsRequest.newBuilder().build();
        ListCollectionsResponse response = stub.listCollections(request);
        return Map.of(
                "success", response.getSuccess(),
                "collections", response.getCollectionsList().stream()
                        .map(c -> Map.of("name", c.getName()))
                        .toList()
        );
    }

    private Object handleGetCollectionInfo(Map<String, Object> params) {
        String collectionName = (String) params.get("collectionName");
        GetCollectionInfoRequest request = GetCollectionInfoRequest.newBuilder()
                .setCollectionName(collectionName)
                .build();
        GetCollectionInfoResponse response = stub.getCollectionInfo(request);
        return Map.of(
                "success", response.getSuccess(),
                "info", Map.of(
                        "name", response.getInfo().getName(),
                        "pointsCount", response.getInfo().getPointsCount(),
                        "status", response.getInfo().getStatus()
                )
        );
    }

    private Object handleHybridSearch(Map<String, Object> params) {
        String queryText = (String) params.get("queryText");
        int limit = params.getOrDefault("limit", 5) instanceof Number n ? n.intValue() : 5;
        boolean summarize = params.getOrDefault("summarize", false) instanceof Boolean b ? b : false;

        HybridSearchRequest.Builder builder = HybridSearchRequest.newBuilder()
                .setQueryText(queryText)
                .setLimit(limit)
                .setSummarize(summarize);

        if (params.containsKey("filters")) {
            Map<String, String> filters = (Map<String, String>) params.get("filters");
            filters.forEach(builder::putFilters);
        }

        HybridSearchResponse response = stub.hybridSearch(builder.build());

        return Map.of(
                "results", response.getResultsList().stream()
                        .map(r -> Map.of(
                                "id", r.getId(),
                                "score", r.getScore(),
                                "payload", r.getPayload(),
                                "collection", r.getCollection(),
                                "metadata", r.getMetadataMap()
                        ))
                        .toList(),
                "summary", response.getSummary(),
                "fallbackUsed", response.getFallbackUsed(),
                "collectionsSearched", response.getCollectionsSearched(),
                "failedCollections", response.getFailedCollectionsList()
        );
    }

    private Object handleIngestDocument(Map<String, Object> params) {
        String documentId = (String) params.get("documentId");
        String content = (String) params.get("content");

        @SuppressWarnings("unchecked")
        java.util.List<String> targetCollections = (java.util.List<String>) params.get("targetCollections");
        if (targetCollections == null) {
            targetCollections = java.util.Collections.emptyList();
        }

        int chunkSize = params.getOrDefault("chunkSize", 512) instanceof Number n ? n.intValue() : 512;
        int chunkOverlap = params.getOrDefault("chunkOverlap", 50) instanceof Number n ? n.intValue() : 50;

        IngestDocumentRequest.Builder builder = IngestDocumentRequest.newBuilder()
                .setDocumentId(documentId)
                .setContent(content)
                .addAllTargetCollections(targetCollections)
                .setChunkingConfig(
                        com.mcp.qdrant.proto.ChunkingConfig.newBuilder()
                                .setChunkSize(chunkSize)
                                .setChunkOverlap(chunkOverlap)
                                .setSeparator("\n")
                                .build()
                );

        if (params.containsKey("metadata")) {
            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) params.get("metadata");
            metadata.forEach(builder::putMetadata);
        }

        IngestDocumentResponse response = stub.ingestDocument(builder.build());

        return Map.of(
                "success", response.getSuccess(),
                "chunksIndexed", response.getChunksIndexed(),
                "indexedCollections", response.getIndexedCollectionsList(),
                "failedCollections", response.getFailedCollectionsList()
        );
    }

    private Object handleCreateCollection(Map<String, Object> params) {
        String collectionName = (String) params.get("collectionName");
        int dimension = params.getOrDefault("dimension", 768) instanceof Number n ? n.intValue() : 768;
        String distance = (String) params.getOrDefault("distance", "Cosine");

        CreateCollectionRequest request = CreateCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .setDimension(dimension)
                .setDistance(distance)
                .build();

        CreateCollectionResponse response = stub.createCollection(request);

        return Map.of(
                "success", response.getSuccess(),
                "collectionName", response.getCollectionName()
        );
    }

    private Object handleDeleteCollection(Map<String, Object> params) {
        String collectionName = (String) params.get("collectionName");

        DeleteCollectionRequest request = DeleteCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .build();

        DeleteCollectionResponse response = stub.deleteCollection(request);

        return Map.of(
                "success", response.getSuccess(),
                "collectionName", response.getCollectionName()
        );
    }
}

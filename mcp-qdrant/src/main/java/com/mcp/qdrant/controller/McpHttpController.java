package com.mcp.qdrant.controller;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.qdrant.proto.BackupCollectionRequest;
import com.mcp.qdrant.proto.BackupCollectionResponse;
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
import com.mcp.qdrant.proto.ListDocumentsRequest;
import com.mcp.qdrant.proto.ListDocumentsResponse;
import com.mcp.qdrant.proto.McpQdrantServiceGrpc;
import com.mcp.qdrant.proto.RestoreCollectionRequest;
import com.mcp.qdrant.proto.RestoreCollectionResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@RestController
@RequestMapping("/mcp")
public class McpHttpController {

    private static final Logger log = LoggerFactory.getLogger(McpHttpController.class);

    private final ObjectMapper objectMapper;
    private final int grpcPort;
    private ManagedChannel channel;
    private McpQdrantServiceGrpc.McpQdrantServiceBlockingStub stub;

    public McpHttpController(ObjectMapper objectMapper, @Value("${grpc.server.port:9091}") int grpcPort) {
        this.objectMapper = objectMapper;
        this.grpcPort = grpcPort;
    }

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder.forAddress("localhost", grpcPort)
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
        Object methodValue = request.get("method");
        Object paramsValue = request.get("params");
        Object id = request.get("id");

        // Wrong JSON types are protocol errors, not transport failures: answer with
        // the JSON-RPC envelope instead of letting the cast escape as an HTTP 500.
        if (methodValue != null && !(methodValue instanceof java.lang.String)) {
            return ResponseEntity.ok(jsonRpcError(id, -32600, "Invalid Request: 'method' must be a string"));
        }
        if (paramsValue != null && !(paramsValue instanceof java.util.Map)) {
            return ResponseEntity.ok(jsonRpcError(id, -32600, "Invalid Request: 'params' must be an object"));
        }
        String method = (java.lang.String) methodValue;
        Map<String, Object> params = (Map<String, Object>) paramsValue;

        log.info("MCP request: method={}, id={}", method, id);

        // Validate required fields
        if (method == null || method.isEmpty()) {
            log.error("MCP request missing required 'method' field");
            return ResponseEntity.ok(jsonRpcError(id, -32600, "Invalid Request: missing required 'method' field"));
        }

        try {
            Object result = dispatchMethod(method, params);
            // Handle null id for notifications - return empty response for notifications
            if (id == null) {
                // This is a notification, no response needed per MCP spec
                return ResponseEntity.ok(java.util.Collections.emptyMap());
            }
            return ResponseEntity.ok(Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", result
            ));
        } catch (Exception e) {
            log.error("MCP error: {}", e.getMessage(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            return ResponseEntity.ok(jsonRpcError(id, -32603, errorMessage));
        }
    }

    // JSON-RPC error envelope. The body is a mutable map so a missing request id
    // serializes as JSON null instead of the string "null".
    private Map<String, Object> jsonRpcError(java.lang.Object id, int code, String message) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", id);
        body.put("error", Map.of("code", code, "message", message));
        return body;
    }

    // JSON numbers arrive as Long, so intValue() silently wraps values above 2^31.
    // Read through longValue() and clamp into the documented protocol range;
    // null or non-number values fall back to the caller's default.
    public static int toInt(java.lang.Object value, int fallback, int min, int max) {
        if (value instanceof java.lang.Number n) {
            long clamped = n.longValue();
            if (clamped < min) {
                return min;
            }
            if (clamped > max) {
                return max;
            }
            return (int) clamped;
        }
        return fallback;
    }

    private Object dispatchMethod(String method, Map<String, Object> params) {
        params = params != null ? params : java.util.Collections.emptyMap();
        return switch (method) {
            case "initialize" -> handleInitialize(params);
            case "notifications/initialized" -> handleNotificationInitialized(params);
            case "notifications/cancelled" -> Map.of(); // Notification, no response needed
            case "tools/list" -> handleToolsList();
            case "tools/call" -> handleToolsCall(params);
            case "listCollections" -> handleListCollections();
            case "listDocuments" -> handleListDocuments(params);
            case "getCollectionInfo" -> handleGetCollectionInfo(params);
            case "hybridSearch" -> handleHybridSearch(params);
            case "ingestDocument" -> handleIngestDocument(params);
            case "deleteDocument" -> handleDeleteDocument(params);
            case "getDocumentInfo" -> handleGetDocumentInfo(params);
            case "rebuildDocumentCache" -> handleRebuildDocumentCache(params);
            case "createCollection" -> handleCreateCollection(params);
            case "deleteCollection" -> handleDeleteCollection(params);
            case "backupCollection" -> handleBackupCollection(params);
            case "restoreCollection" -> handleRestoreCollection(params);
            case "resources/list" -> Map.of("resources", java.util.List.of());
            case "resources/templates/list" -> Map.of("resourceTemplates", java.util.List.of());
            case "prompts/list" -> Map.of("prompts", java.util.List.of());
            default -> throw new RuntimeException("Unknown method: " + method);
        };
    }

    private Object handleInitialize(Map<String, Object> params) {
        // MCP protocol initialization - return server capabilities
        Map<String, Object> serverInfo = Map.of(
            "name", "mcp-qdrant",
            "version", "0.0.2"
        );
        
        Map<String, Object> capabilities = Map.of(
            "tools", Map.of(
                "listChanged", false
            ),
            "collections", Map.of(
                "list", true,
                "create", true,
                "delete", true,
                "backup", true,
                "restore", true
            ),
            "documents", Map.of(
                "ingest", true,
                "list", true,
                "delete", true,
                "search", true
            )
        );
        
        return Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", capabilities,
            "serverInfo", serverInfo
        );
    }

    private Object handleNotificationInitialized(Map<String, Object> params) {
        // MCP notification - client confirms initialization complete
        // This is a notification, not a request requiring a response
        log.info("MCP client initialized notification received");
        return Map.of(); // Return empty result for notifications
    }

    private Object handleToolsList() {
        // Return list of available tools in MCP format
        return Map.of(
            "tools", java.util.List.of(
                Map.of(
                    "name", "hybridSearch",
                    "description", "Perform hybrid search across Qdrant collections",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "queryText", Map.of("type", "string", "description", "Search query"),
                            "limit", Map.of("type", "integer", "description", "Max results"),
                            "summarize", Map.of("type", "boolean", "description", "Summarize results")
                        ),
                        "required", java.util.List.of("queryText")
                    )
                ),
                Map.of(
                    "name", "listCollections",
                    "description", "List all Qdrant collections",
                    "inputSchema", Map.of("type", "object", "properties", Map.of())
                ),
                Map.of(
                    "name", "getCollectionInfo",
                    "description", "Get information about a specific collection",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "collectionName", Map.of("type", "string", "description", "Name of the collection")
                        ),
                        "required", java.util.List.of("collectionName")
                    )
                ),
                Map.of(
                    "name", "createCollection",
                    "description", "Create a new Qdrant collection",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "collectionName", Map.of("type", "string"),
                            "dimension", Map.of("type", "integer"),
                            "distance", Map.of("type", "string")
                        ),
                        "required", java.util.List.of("collectionName")
                    )
                ),
                Map.of(
                    "name", "deleteCollection",
                    "description", "Delete a Qdrant collection",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "collectionName", Map.of("type", "string")
                        ),
                        "required", java.util.List.of("collectionName")
                    )
                ),
                Map.of(
                    "name", "backupCollection",
                    "description", "Backup a Qdrant collection to a snapshot",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "collectionName", Map.of("type", "string")
                        ),
                        "required", java.util.List.of("collectionName")
                    )
                ),
                Map.of(
                    "name", "restoreCollection",
                    "description", "Restore a Qdrant collection from a snapshot",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "collectionName", Map.of("type", "string"),
                            "snapshotPath", Map.of("type", "string"),
                            "overwriteExisting", Map.of("type", "boolean")
                        ),
                        "required", java.util.List.of("collectionName", "snapshotPath")
                    )
                ),
                Map.of(
                    "name", "listDocuments",
                    "description", "List documents in a collection",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "collectionName", Map.of("type", "string"),
                            "limit", Map.of("type", "integer"),
                            "offset", Map.of("type", "integer")
                        )
                    )
                ),
                Map.of(
                    "name", "getDocumentInfo",
                    "description", "Get information about a specific document",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "documentId", Map.of("type", "string"),
                            "collectionName", Map.of("type", "string")
                        ),
                        "required", java.util.List.of("documentId")
                    )
                ),
                Map.of(
                    "name", "ingestDocument",
                    "description", "Ingest a document into Qdrant",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "documentId", Map.of("type", "string"),
                            "content", Map.of("type", "string"),
                            "targetCollections", Map.of("type", "array", "items", Map.of("type", "string")),
                            "chunkSize", Map.of("type", "integer"),
                            "chunkOverlap", Map.of("type", "integer")
                        ),
                        "required", java.util.List.of("documentId", "content")
                    )
                ),
                Map.of(
                    "name", "deleteDocument",
                    "description", "Delete a document from Qdrant",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "documentId", Map.of("type", "string"),
                            "collectionName", Map.of("type", "string")
                        ),
                        "required", java.util.List.of("documentId")
                    )
                ),
                Map.of(
                    "name", "rebuildDocumentCache",
                    "description", "Rebuild the document cache for better performance",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "collectionName", Map.of("type", "string")
                        )
                    )
                )
            )
        );
    }

    private Object handleListCollections() {
        ListCollectionsRequest request = ListCollectionsRequest.newBuilder().build();
        ListCollectionsResponse response = stub.listCollections(request);
        return Map.of(
                "success", response.getSuccess(),
                "collections", response.getCollectionsList().stream()
                        .map(c -> Map.of(
                                "name", c.getName(),
                                "pointsCount", c.getPointsCount(),
                                "vectorDimension", c.getVectorDimension(),
                                "distanceMetric", c.getDistanceMetric(),
                                "status", c.getStatus(),
                                "cacheSize", c.getCacheSize(),
                                "totalDocuments", c.getTotalDocuments(),
                                "embeddingModel", c.getEmbeddingModel()
                        ))
                        .toList(),
                "errorMessage", response.getErrorMessage()
        );
    }

    private Object handleGetCollectionInfo(Map<String, Object> params) {
        String collectionName = (String) params.get("collectionName");
        if (collectionName == null || collectionName.isEmpty()) {
            throw new IllegalArgumentException("collectionName is required");
        }
        GetCollectionInfoRequest request = GetCollectionInfoRequest.newBuilder()
                .setCollectionName(collectionName)
                .build();
        GetCollectionInfoResponse response = stub.getCollectionInfo(request);
        return Map.of(
                "success", response.getSuccess(),
                "info", Map.of(
                        "name", response.getInfo().getName(),
                        "pointsCount", response.getInfo().getPointsCount(),
                        "vectorDimension", response.getInfo().getVectorDimension(),
                        "distanceMetric", response.getInfo().getDistanceMetric(),
                        "status", response.getInfo().getStatus()
                ),
                "errorMessage", response.getErrorMessage()
        );
    }

    private Object handleHybridSearch(Map<String, Object> params) {
        String queryText = (String) params.get("queryText");
        if (queryText == null || queryText.isEmpty()) {
            throw new IllegalArgumentException("queryText is required");
        }
        int limit = toInt(params.get("limit"), 5, 1, 100);
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
                "success", response.getSuccess(),
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
                "failedCollections", response.getFailedCollectionsList(),
                "errorMessage", response.getErrorMessage()
        );
    }

    private Object handleIngestDocument(Map<String, Object> params) {
        String documentId = (String) params.get("documentId");
        String content = (String) params.get("content");
        if (documentId == null || documentId.isEmpty()) {
            throw new IllegalArgumentException("documentId is required");
        }
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("content is required");
        }

        @SuppressWarnings("unchecked")
        java.util.List<String> targetCollections = (java.util.List<String>) params.get("targetCollections");
        if (targetCollections == null) {
            targetCollections = java.util.Collections.emptyList();
        }

        int chunkSize = toInt(params.get("chunkSize"), 512, 0, 100000);
        int chunkOverlap = toInt(params.get("chunkOverlap"), 50, 0, 100000);

        IngestDocumentRequest.Builder builder = IngestDocumentRequest.newBuilder()
                .setDocumentId(documentId)
                .setContent(content)
                .addAllTargetCollections(targetCollections)
                .setChunkingConfig(
                        com.mcp.qdrant.proto.ChunkingConfig.newBuilder()
                                .setChunkSize(chunkSize)
                                .setChunkOverlap(chunkOverlap)
                                .setSeparator("\n\n")
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
        if (collectionName == null || collectionName.isEmpty()) {
            throw new IllegalArgumentException("collectionName is required");
        }
        // 0 means "not specified": the gRPC handler substitutes the configured
        // embedding dimension, so the HTTP bridge must not hardcode 768 here.
        int dimension = toInt(params.get("dimension"), 0, 0, 65536);
        String distance = (String) params.getOrDefault("distance", "Cosine");

        CreateCollectionRequest request = CreateCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .setDimension(dimension)
                .setDistance(distance)
                .build();

        CreateCollectionResponse response = stub.createCollection(request);

        return Map.of(
                "success", response.getSuccess(),
                "collectionName", response.getCollectionName(),
                "errorMessage", response.getErrorMessage()
        );
    }

    private Object handleDeleteCollection(Map<String, Object> params) {
        String collectionName = (String) params.get("collectionName");
        if (collectionName == null || collectionName.isEmpty()) {
            throw new IllegalArgumentException("collectionName is required");
        }

        DeleteCollectionRequest request = DeleteCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .build();

        DeleteCollectionResponse response = stub.deleteCollection(request);

        return Map.of(
                "success", response.getSuccess(),
                "collectionName", response.getCollectionName(),
                "errorMessage", response.getErrorMessage()
        );
    }

    private Object handleBackupCollection(Map<String, Object> params) {
        String collectionName = (String) params.get("collectionName");
        if (collectionName == null || collectionName.isEmpty()) {
            throw new IllegalArgumentException("collectionName is required");
        }
        BackupCollectionRequest request = BackupCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .build();

        BackupCollectionResponse response = stub.backupCollection(request);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", response.getSuccess());
        result.put("collectionName", response.getCollectionName());
        result.put("snapshotName", response.getSnapshotName());
        result.put("sizeBytes", response.getSizeBytes());
        result.put("creationTime", response.getCreationTime());
        result.put("downloadUrl", response.getDownloadUrl());
        String errorMessage = response.getErrorMessage();
        if (errorMessage != null && !errorMessage.isEmpty()) {
            result.put("errorMessage", errorMessage);
        }
        return result;
    }

    private Object handleRestoreCollection(Map<String, Object> params) {
        String collectionName = (String) params.get("collectionName");
        String snapshotPath = (String) params.get("snapshotPath");
        if (collectionName == null || collectionName.isEmpty()) {
            throw new IllegalArgumentException("collectionName is required");
        }
        if (snapshotPath == null || snapshotPath.isEmpty()) {
            throw new IllegalArgumentException("snapshotPath is required");
        }
        boolean overwriteExisting = params.getOrDefault("overwriteExisting", false) instanceof Boolean b ? b : false;

        RestoreCollectionRequest request = RestoreCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .setSnapshotPath(snapshotPath)
                .setOverwriteExisting(overwriteExisting)
                .build();

        RestoreCollectionResponse response = stub.restoreCollection(request);

        return Map.of(
                "success", response.getSuccess(),
                "collectionName", response.getCollectionName(),
                "pointsRestored", response.getPointsRestored(),
                "errorMessage", response.getErrorMessage()
        );
    }

    private Object handleListDocuments(Map<String, Object> params) {
        params = params != null ? params : java.util.Collections.emptyMap();
        String collectionName = (String) params.get("collectionName");
        if (collectionName == null) {
            collectionName = "";
        }
        int limit = toInt(params.get("limit"), 100, 1, 1000);
        int offset = toInt(params.get("offset"), 0, 0, java.lang.Integer.MAX_VALUE);

        ListDocumentsRequest request = ListDocumentsRequest.newBuilder()
                .setCollectionName(collectionName)
                .setLimit(limit)
                .setOffset(offset)
                .build();

        ListDocumentsResponse response = stub.listDocuments(request);

        return Map.of(
                "success", response.getSuccess(),
                "totalDocuments", response.getTotalDocuments(),
                "documents", response.getDocumentsList().stream()
                        .map(d -> Map.of(
                                "documentId", d.getDocumentId(),
                                "collection", d.getCollection(),
                                "chunkCount", d.getChunkCount()
                        ))
                        .toList(),
                "errorMessage", response.getErrorMessage()
        );
    }

    private Object handleDeleteDocument(Map<String, Object> params) {
        String documentId = (String) params.get("documentId");
        if (documentId == null || documentId.isEmpty()) {
            throw new IllegalArgumentException("documentId is required");
        }
        String collectionName = (String) params.getOrDefault("collectionName", "");

        com.mcp.qdrant.proto.DeleteDocumentRequest request = 
                com.mcp.qdrant.proto.DeleteDocumentRequest.newBuilder()
                        .setDocumentId(documentId)
                        .setCollectionName(collectionName)
                        .build();

        com.mcp.qdrant.proto.DeleteDocumentResponse response = stub.deleteDocument(request);

        return Map.of(
                "success", response.getSuccess(),
                "documentId", response.getDocumentId(),
                "deletedChunks", response.getDeletedChunks(),
                "collection", response.getCollection(),
                "errorMessage", response.getErrorMessage()
        );
    }

    private Object handleGetDocumentInfo(Map<String, Object> params) {
        String documentId = (String) params.get("documentId");
        if (documentId == null || documentId.isEmpty()) {
            throw new IllegalArgumentException("documentId is required");
        }
        String collectionName = (String) params.getOrDefault("collectionName", "");

        com.mcp.qdrant.proto.GetDocumentInfoRequest request = 
                com.mcp.qdrant.proto.GetDocumentInfoRequest.newBuilder()
                        .setDocumentId(documentId)
                        .setCollectionName(collectionName)
                        .build();

        com.mcp.qdrant.proto.GetDocumentInfoResponse response = stub.getDocumentInfo(request);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", response.getSuccess());
        result.put("exists", response.getExists());
        
        if (response.hasDocumentInfo()) {
            com.mcp.qdrant.proto.DocumentInfo info = response.getDocumentInfo();
            result.put("documentInfo", Map.of(
                    "documentId", info.getDocumentId(),
                    "collection", info.getCollection(),
                    "chunkCount", info.getChunkCount()
            ));
        }
        result.put("errorMessage", response.getErrorMessage());
        
        return result;
    }

    private Object handleRebuildDocumentCache(Map<String, Object> params) {
        params = params != null ? params : java.util.Collections.emptyMap();
        String collectionName = (String) params.getOrDefault("collectionName", "");

        com.mcp.qdrant.proto.RebuildDocumentCacheRequest request = 
                com.mcp.qdrant.proto.RebuildDocumentCacheRequest.newBuilder()
                        .setCollectionName(collectionName)
                        .build();

        com.mcp.qdrant.proto.RebuildDocumentCacheResponse response = stub.rebuildDocumentCache(request);

        return Map.of(
                "success", response.getSuccess(),
                "totalDocuments", response.getTotalDocuments(),
                "collectionsScanned", response.getCollectionsScanned(),
                "errorMessage", response.getErrorMessage()
        );
    }

    private Object handleToolsCall(Map<String, Object> params) {
        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", java.util.Collections.emptyMap());

        if (toolName == null || toolName.isEmpty()) {
            throw new IllegalArgumentException("Tool name is required");
        }

        // Handle edge case where tools/list is incorrectly called via tools/call
        if ("tools/list".equals(toolName)) {
            log.warn("Received tools/list as tool name, redirecting to handleToolsList()");
            return handleToolsList();
        }

        log.info("Executing tool: {}", toolName);

        try {
            Object toolResult = switch (toolName) {
                case "listCollections" -> handleListCollections();
                case "listDocuments" -> handleListDocuments(arguments);
                case "getCollectionInfo" -> handleGetCollectionInfo(arguments);
                case "hybridSearch" -> handleHybridSearch(arguments);
                case "ingestDocument" -> handleIngestDocument(arguments);
                case "deleteDocument" -> handleDeleteDocument(arguments);
                case "getDocumentInfo" -> handleGetDocumentInfo(arguments);
                case "rebuildDocumentCache" -> handleRebuildDocumentCache(arguments);
                case "createCollection" -> handleCreateCollection(arguments);
                case "deleteCollection" -> handleDeleteCollection(arguments);
                case "backupCollection" -> handleBackupCollection(arguments);
                case "restoreCollection" -> handleRestoreCollection(arguments);
                default -> throw new RuntimeException("Unknown tool: " + toolName);
            };

            // Return proper MCP tools/call response format with content array
            return Map.of(
                "content", java.util.List.of(
                    Map.of(
                        "type", "text",
                        "text", objectMapper.writeValueAsString(toolResult)
                    )
                ),
                "isError", false
            );
        } catch (Exception e) {
            log.error("Tool execution failed: {}", e.getMessage(), e);
            try {
                return Map.of(
                    "content", java.util.List.of(
                        Map.of(
                            "type", "text",
                            "text", objectMapper.writeValueAsString(Map.of("error", e.getMessage()))
                        )
                    ),
                    "isError", true
                );
            } catch (Exception jsonError) {
                // Fallback if JSON serialization fails
                return Map.of(
                    "content", java.util.List.of(
                        Map.of(
                            "type", "text",
                            "text", "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}"
                        )
                    ),
                    "isError", true
                );
            }
        }
    }
}

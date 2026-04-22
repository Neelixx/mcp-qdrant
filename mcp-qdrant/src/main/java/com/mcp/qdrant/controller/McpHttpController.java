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
                            "collectionName", Map.of("type", "string"),
                            "backupPath", Map.of("type", "string")
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
                        .toList()
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
                        "status", response.getInfo().getStatus()
                )
        );
    }

    private Object handleHybridSearch(Map<String, Object> params) {
        String queryText = (String) params.get("queryText");
        if (queryText == null || queryText.isEmpty()) {
            throw new IllegalArgumentException("queryText is required");
        }
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
                                .setSeparator("\\n\\n")
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
        if (collectionName == null || collectionName.isEmpty()) {
            throw new IllegalArgumentException("collectionName is required");
        }

        DeleteCollectionRequest request = DeleteCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .build();

        DeleteCollectionResponse response = stub.deleteCollection(request);

        return Map.of(
                "success", response.getSuccess(),
                "collectionName", response.getCollectionName()
        );
    }

    private Object handleBackupCollection(Map<String, Object> params) {
        String collectionName = (String) params.get("collectionName");
        if (collectionName == null || collectionName.isEmpty()) {
            throw new IllegalArgumentException("collectionName is required");
        }
        String backupPath = (String) params.getOrDefault("backupPath", "");

        BackupCollectionRequest request = BackupCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .setBackupPath(backupPath)
                .build();

        BackupCollectionResponse response = stub.backupCollection(request);

        return Map.of(
                "success", response.getSuccess(),
                "collectionName", response.getCollectionName(),
                "snapshotName", response.getSnapshotName(),
                "sizeBytes", response.getSizeBytes(),
                "creationTime", response.getCreationTime(),
                "downloadUrl", response.getDownloadUrl()
        );
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
                "pointsRestored", response.getPointsRestored()
        );
    }

    private Object handleListDocuments(Map<String, Object> params) {
        params = params != null ? params : java.util.Collections.emptyMap();
        String collectionName = (String) params.get("collectionName");
        if (collectionName == null) {
            collectionName = "";
        }
        int limit = params.getOrDefault("limit", 100) instanceof Number n ? n.intValue() : 100;
        int offset = params.getOrDefault("offset", 0) instanceof Number n ? n.intValue() : 0;

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
        
        return switch (toolName) {
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
    }
}

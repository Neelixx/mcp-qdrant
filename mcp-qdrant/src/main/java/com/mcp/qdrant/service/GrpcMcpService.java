package com.mcp.qdrant.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import com.mcp.qdrant.chunker.TextChunker;
import com.mcp.qdrant.client.EmbeddingServiceClient;
import com.mcp.qdrant.config.CacheConfig;
import com.mcp.qdrant.config.EmbeddingProperties;
import com.mcp.qdrant.config.QdrantProperties;
import com.mcp.qdrant.model.DocumentChunk;
import com.mcp.qdrant.model.SearchResult;
import com.mcp.qdrant.proto.BackupCollectionRequest;
import com.mcp.qdrant.proto.BackupCollectionResponse;
import com.mcp.qdrant.proto.CollectionInfo;
import com.mcp.qdrant.proto.CreateCollectionRequest;
import com.mcp.qdrant.proto.CreateCollectionResponse;
import com.mcp.qdrant.proto.DeleteCollectionRequest;
import com.mcp.qdrant.proto.DeleteCollectionResponse;
import com.mcp.qdrant.proto.DocumentInfo;
import com.mcp.qdrant.proto.DocumentSource;
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
import com.mcp.qdrant.proto.SearchResult.Builder;
import com.mcp.qdrant.repository.QdrantRepository;

import io.grpc.stub.StreamObserver;
import io.qdrant.client.grpc.Collections;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class GrpcMcpService extends McpQdrantServiceGrpc.McpQdrantServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcMcpService.class);
    
    private final EmbeddingServiceClient embeddingClient;
    private final QdrantRepository qdrantRepository;
    private final QdrantProperties qdrantProperties;
    private final EmbeddingProperties embeddingProperties;
    private final TextChunker textChunker;
    private final SummarizationService summarizationService;
    private final WebClient webClient;
    private final CacheConfig cacheConfig;
    
    public GrpcMcpService(EmbeddingServiceClient embeddingClient, QdrantRepository qdrantRepository,
                          QdrantProperties qdrantProperties, EmbeddingProperties embeddingProperties,
                          TextChunker textChunker, SummarizationService summarizationService,
                          CacheConfig cacheConfig) {
        this.embeddingClient = embeddingClient;
        this.qdrantRepository = qdrantRepository;
        this.qdrantProperties = qdrantProperties;
        this.embeddingProperties = embeddingProperties;
        this.textChunker = textChunker;
        this.summarizationService = summarizationService;
        this.webClient = createWebClient();
        this.cacheConfig = cacheConfig;
    }
    
    private WebClient createWebClient() {
        String baseUrl = String.format("%s://%s:%d",
                qdrantProperties.isUseTls() ? "https" : "http",
                qdrantProperties.getHost(),
                qdrantProperties.getHttpPort());
        
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl);
        
        // Add API key if configured
        if (qdrantProperties.getApiKey() != null && !qdrantProperties.getApiKey().isEmpty()) {
            builder.defaultHeader("api-key", qdrantProperties.getApiKey());
        }
        
        return builder.build();
    }

    @Override
    public void hybridSearch(HybridSearchRequest request, StreamObserver<HybridSearchResponse> responseObserver) {
        try {
            // Validate query text
            if (request.getQueryText() == null || request.getQueryText().trim().isEmpty()) {
                throw new IllegalArgumentException("Query text is required and cannot be empty");
            }

            List<SearchResult> allResults = new ArrayList<>();
            int collectionsSearched = 0;
            List<String> failedCollections = new ArrayList<>();

            // Handle default limit (num_results) - default to 5 if not specified
            int limit = request.hasLimit() && request.getLimit() > 0 ? request.getLimit() : 5;

            // Determine if summarization should be used
            // Only summarize if: explicitly requested AND summarize model is configured/available
            String summarizeModel = qdrantProperties.getSummarizeModel();
            boolean modelAvailable = summarizeModel != null 
                    && !summarizeModel.isEmpty()
                    && !"null".equals(summarizeModel);
            boolean summarizeRequested = request.hasSummarize() && request.getSummarize();
            boolean shouldSummarize = summarizeRequested && modelAvailable;

            List<String> collectionsToSearch = getCollectionsToSearch();
            
            // Group collections by embedding model
            Map<String, List<String>> collectionsByModel = new java.util.HashMap<>();
            for (String collection : collectionsToSearch) {
                EmbeddingProperties.CollectionEmbeddingConfig config = embeddingProperties.getConfigForCollection(collection);
                String modelName = (config != null && config.getModel() != null) 
                    ? config.getModel() 
                    : embeddingProperties.getModel();
                collectionsByModel.computeIfAbsent(modelName, k -> new ArrayList<>()).add(collection);
            }
            
            // Search each model group with appropriate embedding
            for (Map.Entry<String, List<String>> entry : collectionsByModel.entrySet()) {
                String modelName = entry.getKey();
                List<String> collectionsForModel = entry.getValue();
                
                try {
                    float[] queryVector = embeddingClient.embedWithModel(request.getQueryText(), modelName);
                    
                    for (String collection : collectionsForModel) {
                        try {
                            List<SearchResult> results = qdrantRepository.search(
                                    collection,
                                    queryVector,
                                    limit,
                                    request.getFiltersMap()
                            );
                            allResults.addAll(results);
                            collectionsSearched++;
                        } catch (Exception e) {
                            log.error("Search failed for collection {}: {}", collection, e.getMessage());
                            failedCollections.add(collection);
                        }
                    }
                } catch (Exception e) {
                    log.error("Embedding generation failed for model '{}': {}", modelName, e.getMessage());
                    failedCollections.addAll(collectionsForModel);
                }
            }

            List<SearchResult> sortedResults = allResults.stream()
                    .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                    .limit(limit)
                    .collect(Collectors.toList());

            String summary = "";
            boolean fallbackUsed = false;

            if (shouldSummarize && !sortedResults.isEmpty()) {
                try {
                    summary = summarizationService.summarize(request.getQueryText(), sortedResults);
                } catch (Exception e) {
                    log.error("Summarization failed: {}", e.getMessage());
                    fallbackUsed = true;
                }
            } else if (summarizeRequested && !modelAvailable) {
                // User requested summary but model is not available
                fallbackUsed = true;
                log.info("Summarization skipped: embedding model not configured or empty");
            }

            HybridSearchResponse response = buildResponse(sortedResults, summary, fallbackUsed, collectionsSearched, failedCollections);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Hybrid search failed: {}", e.getMessage(), e);
            responseObserver.onNext(HybridSearchResponse.newBuilder()
                    .setFallbackUsed(true)
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void ingestDocument(IngestDocumentRequest request, StreamObserver<IngestDocumentResponse> responseObserver) {
        try {
            List<String> targetCollections;
            if (!request.getTargetCollectionsList().isEmpty()) {
                targetCollections = request.getTargetCollectionsList();
            } else if (qdrantProperties.isAllCollections()) {
                // When 'all' is specified and no explicit targets, ingest to all existing collections
                targetCollections = qdrantRepository.getQdrantClient().listCollectionsAsync()
                        .get(qdrantProperties.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
                if (targetCollections.isEmpty()) {
                    throw new RuntimeException("No collections available for ingestion. Please create a collection first.");
                }
            } else {
                targetCollections = qdrantProperties.getCollections();
            }

            List<DocumentChunk> chunks = textChunker.chunk(
                    request.getContent(),
                    request.getMetadataMap(),
                    request.hasChunkingConfig() ? request.getChunkingConfig() : null
            );

            List<float[]> vectors = embeddingClient.embedBatch(
                    chunks.stream().map(DocumentChunk::getContent).toList()
            );

            int totalIndexed = 0;
            List<String> failedCollections = new ArrayList<>();
            for (String collection : targetCollections) {
                try {
                    int indexed = qdrantRepository.batchIndex(
                            collection,
                            request.getDocumentId(),
                            chunks,
                            vectors,
                            embeddingProperties.getDimension()
                    );
                    totalIndexed += indexed;
                } catch (Exception e) {
                    log.error("Indexing failed for collection {}: {}", collection, e.getMessage());
                    failedCollections.add(collection);
                }
            }

            IngestDocumentResponse response = IngestDocumentResponse.newBuilder()
                    .setSuccess(failedCollections.isEmpty())
                    .setChunksIndexed(totalIndexed)
                    .addAllIndexedCollections(targetCollections)
                    .addAllFailedCollections(failedCollections)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Document ingestion failed: {}", e.getMessage(), e);
            responseObserver.onNext(IngestDocumentResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    private HybridSearchResponse buildResponse(List<SearchResult> results, String summary,
                                                boolean fallbackUsed, int collectionsSearched, List<String> failedCollections) {
        HybridSearchResponse.Builder builder = HybridSearchResponse.newBuilder()
                .setFallbackUsed(fallbackUsed)
                .setCollectionsSearched(collectionsSearched);

        if (!failedCollections.isEmpty()) {
            builder.addAllFailedCollections(failedCollections);
        }

        if (summary != null && !summary.isEmpty()) {
            builder.setSummary(summary);
        }

        // Track document sources for the table
        Map<String, Map<String, Integer>> sourceCounts = new java.util.HashMap<>();

        for (SearchResult result : results) {
            Builder resultBuilder = com.mcp.qdrant.proto.SearchResult.newBuilder()
                    .setId(result.getId())
                    .setScore(result.getScore())
                    .setPayload(result.getPayload())
                    .setCollection(result.getCollection());

            if (result.getMetadata() != null) {
                resultBuilder.putAllMetadata(result.getMetadata());
                
                // Track source document
                String docId = result.getMetadata().getOrDefault("document_id", "unknown");
                String collection = result.getCollection();
                sourceCounts.computeIfAbsent(collection, k -> new java.util.HashMap<>())
                           .merge(docId, 1, Integer::sum);
            }

            builder.addResults(resultBuilder.build());
        }

        // Add document sources table
        for (Map.Entry<String, Map<String, Integer>> collectionEntry : sourceCounts.entrySet()) {
            String collection = collectionEntry.getKey();
            for (Map.Entry<String, Integer> docEntry : collectionEntry.getValue().entrySet()) {
                DocumentSource source = DocumentSource.newBuilder()
                        .setDocumentId(docEntry.getKey())
                        .setCollection(collection)
                        .setChunkCount(docEntry.getValue())
                        .build();
                builder.addSources(source);
            }
        }

        return builder.build();
    }

    @Override
    public void createCollection(CreateCollectionRequest request, StreamObserver<CreateCollectionResponse> responseObserver) {
        try {
            String collectionName = request.getCollectionName();
            int dimension = request.getDimension() > 0 ? request.getDimension() : embeddingProperties.getDimension();
            
            Collections.Distance distance = Collections.Distance.Cosine;
            if ("Euclid".equals(request.getDistance())) {
                distance = Collections.Distance.Euclid;
            } else if ("Dot".equals(request.getDistance())) {
                distance = Collections.Distance.Dot;
            }

            Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                    .setDistance(distance)
                    .setSize(dimension)
                    .build();

            qdrantRepository.getQdrantClient().createCollectionAsync(collectionName, vectorParams).get(10, java.util.concurrent.TimeUnit.SECONDS);
            
            // Initialize cache for the new collection with default size 1000
            cacheConfig.initializeCollectionCache(collectionName);
            
            CreateCollectionResponse response = CreateCollectionResponse.newBuilder()
                    .setSuccess(true)
                    .setCollectionName(collectionName)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Created collection: {} with dimension: {}, distance: {}", collectionName, dimension, distance);

        } catch (Exception e) {
            log.error("Failed to create collection: {}", e.getMessage(), e);
            responseObserver.onNext(CreateCollectionResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deleteCollection(DeleteCollectionRequest request, StreamObserver<DeleteCollectionResponse> responseObserver) {
        try {
            String collectionName = request.getCollectionName();
            
            qdrantRepository.getQdrantClient().deleteCollectionAsync(collectionName).get(10, java.util.concurrent.TimeUnit.SECONDS);
            
            // Delete cache for the collection
            cacheConfig.deleteCollectionCache(collectionName);
            
            DeleteCollectionResponse response = DeleteCollectionResponse.newBuilder()
                    .setSuccess(true)
                    .setCollectionName(collectionName)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Deleted collection: {}", collectionName);

        } catch (Exception e) {
            log.error("Failed to delete collection: {}", e.getMessage(), e);
            responseObserver.onNext(DeleteCollectionResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listCollections(ListCollectionsRequest request, StreamObserver<ListCollectionsResponse> responseObserver) {
        try {
            java.util.List<String> collectionNames = qdrantRepository.getQdrantClient().listCollectionsAsync().get(10, java.util.concurrent.TimeUnit.SECONDS);
            
            ListCollectionsResponse.Builder responseBuilder = ListCollectionsResponse.newBuilder().setSuccess(true);
            
            for (String name : collectionNames) {
                // Get collection info from Qdrant
                var collectionInfo = qdrantRepository.getQdrantClient().getCollectionInfoAsync(name)
                        .get(qdrantProperties.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
                
                // Get cache size for this collection
                long cacheSize = cacheConfig.getCacheSizeForCollection(name);
                
                // Get total document count for this collection
                int totalDocuments = 0;
                try {
                    var documents = qdrantRepository.listDocuments(name, Integer.MAX_VALUE, 0);
                    totalDocuments = documents.size();
                } catch (Exception e) {
                    log.warn("Could not count documents for collection {}: {}", name, e.getMessage());
                }
                
                // Get vector dimension and embedding model from configuration
                int vectorDimension = embeddingProperties.getDimension();
                String embeddingModel = embeddingProperties.getModel();
                
                // Check for collection-specific config
                var collectionEmbeddingConfig = embeddingProperties.getConfigForCollection(name);
                if (collectionEmbeddingConfig != null) {
                    if (collectionEmbeddingConfig.getDimension() > 0) {
                        vectorDimension = collectionEmbeddingConfig.getDimension();
                    }
                    if (collectionEmbeddingConfig.getModel() != null) {
                        embeddingModel = collectionEmbeddingConfig.getModel();
                    }
                }
                
                String distanceMetric = "Cosine"; // Default, could be enhanced later
                
                CollectionInfo info = CollectionInfo.newBuilder()
                        .setName(name)
                        .setPointsCount((int) collectionInfo.getPointsCount())
                        .setVectorDimension(vectorDimension)
                        .setDistanceMetric(distanceMetric)
                        .setStatus(collectionInfo.getStatus().name())
                        .setCacheSize(cacheSize)
                        .setTotalDocuments(totalDocuments)
                        .setEmbeddingModel(embeddingModel)
                        .build();
                responseBuilder.addCollections(info);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("Listed {} collections with cache sizes and document counts", collectionNames.size());

        } catch (Exception e) {
            log.error("Failed to list collections: {}", e.getMessage(), e);
            responseObserver.onNext(ListCollectionsResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * Returns the list of collections to search based on configuration.
     * If 'all' is specified, fetches all collections from Qdrant.
     * Otherwise returns the configured collection list.
     */
    private List<String> getCollectionsToSearch() throws Exception {
        if (qdrantProperties.isAllCollections()) {
            return qdrantRepository.getQdrantClient().listCollectionsAsync()
                    .get(qdrantProperties.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        return qdrantProperties.getCollections();
    }

    @Override
    public void getCollectionInfo(GetCollectionInfoRequest request, StreamObserver<GetCollectionInfoResponse> responseObserver) {
        try {
            String collectionName = request.getCollectionName();
            
            var collectionInfo = qdrantRepository.getQdrantClient().getCollectionInfoAsync(collectionName).get(5, java.util.concurrent.TimeUnit.SECONDS);
            
            CollectionInfo info = CollectionInfo.newBuilder()
                    .setName(collectionName)
                    .setPointsCount((int) collectionInfo.getPointsCount())
                    .setStatus(collectionInfo.getStatus().name())
                    .build();
            
            GetCollectionInfoResponse response = GetCollectionInfoResponse.newBuilder()
                    .setSuccess(true)
                    .setInfo(info)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Retrieved info for collection: {}", collectionName);

        } catch (Exception e) {
            log.error("Failed to get collection info: {}", e.getMessage(), e);
            responseObserver.onNext(GetCollectionInfoResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void backupCollection(BackupCollectionRequest request, StreamObserver<BackupCollectionResponse> responseObserver) {
        try {
            String collectionName = request.getCollectionName();
            log.info("Starting backup for collection: {}", collectionName);
            
            // Check if collection exists first
            boolean exists = qdrantRepository.getQdrantClient().collectionExistsAsync(collectionName)
                    .get(qdrantProperties.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!exists) {
                throw new RuntimeException("Collection not found: " + collectionName);
            }
            
            // Create snapshot via Qdrant API
            var snapshotInfo = qdrantRepository.getQdrantClient().createSnapshotAsync(collectionName)
                    .get(qdrantProperties.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
            
            // Construct download URL
            String protocol = qdrantProperties.isUseTls() ? "https" : "http";
            String downloadUrl = String.format("%s://%s:%d/collections/%s/snapshots/%s",
                    protocol,
                    qdrantProperties.getHost(),
                    qdrantProperties.getHttpPort(),
                    collectionName,
                    snapshotInfo.getName());
            
            BackupCollectionResponse response = BackupCollectionResponse.newBuilder()
                    .setSuccess(true)
                    .setCollectionName(collectionName)
                    .setSnapshotName(snapshotInfo.getName())
                    .setSizeBytes(snapshotInfo.getSize())
                    .setCreationTime(snapshotInfo.getCreationTime().toString())
                    .setDownloadUrl(downloadUrl)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Backup completed for collection: {} - snapshot: {} ({} bytes)", 
                    collectionName, snapshotInfo.getName(), snapshotInfo.getSize());

        } catch (Exception e) {
            log.error("Failed to backup collection: {}", e.getMessage(), e);
            responseObserver.onNext(BackupCollectionResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void restoreCollection(RestoreCollectionRequest request, StreamObserver<RestoreCollectionResponse> responseObserver) {
        try {
            String collectionName = request.getCollectionName();
            String snapshotPath = request.getSnapshotPath();
            boolean overwriteExisting = request.getOverwriteExisting();
            
            log.info("Starting restore for collection: {} from snapshot: {}", collectionName, snapshotPath);
            
            // Validate snapshot file exists
            java.nio.file.Path snapshotFile = java.nio.file.Path.of(snapshotPath);
            if (!java.nio.file.Files.exists(snapshotFile)) {
                throw new RuntimeException("Snapshot file not found: " + snapshotPath);
            }
            
            // Check if collection exists and delete if overwrite is requested
            if (overwriteExisting) {
                try {
                    boolean exists = qdrantRepository.getQdrantClient().collectionExistsAsync(collectionName)
                            .get(qdrantProperties.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (exists) {
                        log.info("Deleting existing collection: {}", collectionName);
                        qdrantRepository.getQdrantClient().deleteCollectionAsync(collectionName)
                                .get(qdrantProperties.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
                    }
                } catch (Exception e) {
                    log.warn("Could not delete existing collection: {}", e.getMessage());
                }
            }
            
            // Upload and restore snapshot via Qdrant HTTP API using pre-configured WebClient
            log.info("Uploading snapshot for collection: {}", collectionName);
            
            org.springframework.core.io.FileSystemResource fileResource = 
                    new org.springframework.core.io.FileSystemResource(snapshotFile.toFile());
            
            String response = webClient.post()
                    .uri("/collections/{collection}/snapshots/upload?priority=snapshot", collectionName)
                    .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                    .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(
                            "snapshot", fileResource))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofMillis(qdrantProperties.getTimeoutMs() * 10)) // Longer timeout for upload
                    .block();
            
            if (response == null || !response.contains("\"status\":\"ok\"")) {
                throw new RuntimeException("Snapshot upload failed: " + response);
            }
            
            log.info("Snapshot uploaded successfully, response: {}", response);
            
            // Wait for restore to complete and verify
            var collectionInfo = qdrantRepository.getQdrantClient().getCollectionInfoAsync(collectionName)
                    .get(qdrantProperties.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
            
            long pointsCount = collectionInfo.getPointsCount();
            log.info("Restore completed for collection: {} with {} points", collectionName, pointsCount);
            
            // Check for overflow
            int pointsRestored = pointsCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pointsCount;
            
            // Rebuild document cache after restore
            qdrantRepository.rebuildDocumentCache(collectionName);
            
            RestoreCollectionResponse restoreResponse = RestoreCollectionResponse.newBuilder()
                    .setSuccess(true)
                    .setCollectionName(collectionName)
                    .setPointsRestored(pointsRestored)
                    .build();
            
            responseObserver.onNext(restoreResponse);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to restore collection: {}", e.getMessage(), e);
            responseObserver.onNext(RestoreCollectionResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listDocuments(ListDocumentsRequest request,
                              StreamObserver<ListDocumentsResponse> responseObserver) {
        try {
            String collectionName = request.getCollectionName();
            int limit = request.getLimit() > 0 ? request.getLimit() : 100;
            int offset = request.getOffset() >= 0 ? request.getOffset() : 0;
            
            log.info("Listing documents from collection: '{}', offset: {}, limit: {}", 
                    collectionName.isEmpty() ? "all" : collectionName, offset, limit);
            
            java.util.List<com.mcp.qdrant.repository.QdrantRepository.DocumentInfo> documents = 
                    qdrantRepository.listDocuments(collectionName, limit, offset);
            
            ListDocumentsResponse.Builder responseBuilder = 
                    ListDocumentsResponse.newBuilder()
                            .setSuccess(true)
                            .setTotalDocuments(documents.size());
            
            for (com.mcp.qdrant.repository.QdrantRepository.DocumentInfo info : documents) {
                DocumentInfo docInfo = DocumentInfo.newBuilder()
                        .setDocumentId(info.getDocumentId())
                        .setCollection(info.getCollection())
                        .setChunkCount(info.getChunkCount())
                        .build();
                responseBuilder.addDocuments(docInfo);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("Listed {} documents", documents.size());

        } catch (Exception e) {
            log.error("Failed to list documents: {}", e.getMessage(), e);
            responseObserver.onNext(ListDocumentsResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deleteDocument(com.mcp.qdrant.proto.DeleteDocumentRequest request,
                                  io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.DeleteDocumentResponse> responseObserver) {
        try {
            String documentId = request.getDocumentId();
            String collectionName = request.getCollectionName();
            
            log.info("Deleting document: '{}' from collection: '{}'", 
                    documentId, collectionName.isEmpty() ? "all" : collectionName);
            
            int deletedChunks = qdrantRepository.deleteDocument(documentId, collectionName);
            
            com.mcp.qdrant.proto.DeleteDocumentResponse response = 
                    com.mcp.qdrant.proto.DeleteDocumentResponse.newBuilder()
                            .setSuccess(deletedChunks > 0)
                            .setDocumentId(documentId)
                            .setDeletedChunks(deletedChunks)
                            .setCollection(collectionName)
                            .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Deleted {} chunks for document '{}'", deletedChunks, documentId);

        } catch (Exception e) {
            log.error("Failed to delete document: {}", e.getMessage(), e);
            responseObserver.onNext(com.mcp.qdrant.proto.DeleteDocumentResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getDocumentInfo(com.mcp.qdrant.proto.GetDocumentInfoRequest request,
                                 io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.GetDocumentInfoResponse> responseObserver) {
        try {
            String documentId = request.getDocumentId();
            String collectionName = request.getCollectionName();
            
            log.info("Getting document info for: '{}' in collection: '{}'", 
                    documentId, collectionName.isEmpty() ? "all" : collectionName);
            
            com.mcp.qdrant.repository.QdrantRepository.DocumentInfo info = 
                    qdrantRepository.getDocumentInfo(documentId, collectionName);
            
            com.mcp.qdrant.proto.GetDocumentInfoResponse.Builder responseBuilder = 
                    com.mcp.qdrant.proto.GetDocumentInfoResponse.newBuilder()
                            .setSuccess(true)
                            .setExists(info != null);
            
            if (info != null) {
                com.mcp.qdrant.proto.DocumentInfo docInfo = com.mcp.qdrant.proto.DocumentInfo.newBuilder()
                        .setDocumentId(info.getDocumentId())
                        .setCollection(info.getCollection())
                        .setChunkCount(info.getChunkCount())
                        .build();
                responseBuilder.setDocumentInfo(docInfo);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("Document '{}' exists: {}", documentId, info != null);

        } catch (Exception e) {
            log.error("Failed to get document info: {}", e.getMessage(), e);
            responseObserver.onNext(com.mcp.qdrant.proto.GetDocumentInfoResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void rebuildDocumentCache(com.mcp.qdrant.proto.RebuildDocumentCacheRequest request,
                                        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.RebuildDocumentCacheResponse> responseObserver) {
        try {
            String collectionName = request.getCollectionName();
            
            log.info("Rebuilding document cache for collection: '{}'", 
                    collectionName.isEmpty() ? "all" : collectionName);
            
            int totalDocuments = qdrantRepository.rebuildDocumentCache(collectionName);
            
            int collectionsScanned = 0;
            if (collectionName != null && !collectionName.isEmpty()) {
                collectionsScanned = 1;
            }
            
            com.mcp.qdrant.proto.RebuildDocumentCacheResponse response = 
                    com.mcp.qdrant.proto.RebuildDocumentCacheResponse.newBuilder()
                            .setSuccess(true)
                            .setTotalDocuments(totalDocuments)
                            .setCollectionsScanned(collectionsScanned)
                            .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Document cache rebuilt. Found {} documents", totalDocuments);

        } catch (Exception e) {
            log.error("Failed to rebuild document cache: {}", e.getMessage(), e);
            responseObserver.onNext(com.mcp.qdrant.proto.RebuildDocumentCacheResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
}

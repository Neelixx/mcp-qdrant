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

    /**
     * Resolves a client-supplied distance metric name to the Qdrant enum,
     * case-insensitively. A null/blank name means "not specified" and keeps the
     * caller's default; anything that is not one of the metrics the Qdrant
     * Distance enum supports is rejected, so a typo cannot silently index a
     * collection with the wrong distance.
     */
    public static Collections.Distance resolveDistance(String distance, Collections.Distance fallback) {
        if (distance == null || distance.trim().isEmpty()) {
            return fallback;
        }
        String name = distance.trim().toLowerCase();
        List<String> supported = new ArrayList<>();
        for (Collections.Distance candidate : Collections.Distance.values()) {
            supported.add(candidate.name());
            if (candidate.name().toLowerCase().equals(name)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported distance metric: '" + distance
                + "' (supported: " + String.join(", ", supported) + ")");
    }

    /**
     * Resolves a client-supplied limit: non-positive values fall back to the
     * default; anything above max is capped so a single request cannot force
     * Qdrant to materialise an unbounded result set.
     */
    public static int clampLimit(int requested, int fallback, int max) {
        if (requested <= 0) {
            return fallback;
        }
        return Math.min(requested, max);
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

            // Handle default limit (num_results): the configured search limit is the
            // fallback when the client does not ask for one, defaulting to 5 when the
            // configured value is unset; anything above 100 is capped so one client
            // request cannot ask Qdrant to materialise the whole collection.
            int configuredLimit = qdrantProperties.getSearchLimit() > 0 ? qdrantProperties.getSearchLimit() : 5;
            int limit = clampLimit(request.hasLimit() ? request.getLimit() : 0, configuredLimit, 100);

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

            // Total failure means nothing could be searched at all: either there was
            // nothing to search, or every collection we tried failed (search error or
            // embedding generation for its model). Zero hits from a successful search
            // is NOT a failure, so it keeps success=true.
            int attemptedCollections = collectionsToSearch.size();
            boolean totalFailure = attemptedCollections == 0
                    || (collectionsSearched == 0 && !failedCollections.isEmpty());
            String searchError = null;
            if (totalFailure) {
                searchError = attemptedCollections == 0
                        ? "No collections available to search"
                        : "Search failed for all " + failedCollections.size() + " collections: "
                                + String.join(", ", failedCollections);
            }

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

            HybridSearchResponse response = buildResponse(sortedResults, summary, fallbackUsed,
                    collectionsSearched, failedCollections, !totalFailure, searchError);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Hybrid search failed: {}", e.getMessage(), e);
            responseObserver.onNext(HybridSearchResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
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

            // Group target collections by the embedding model resolved for each one
            // (falling back to the default model), mirroring hybridSearch: chunks have
            // to be embedded with the model the collection uses for search, otherwise
            // the stored vectors and the query vectors are not comparable.
            Map<String, List<String>> collectionsByModel = new java.util.HashMap<>();
            for (String collection : targetCollections) {
                EmbeddingProperties.CollectionEmbeddingConfig config = embeddingProperties.getConfigForCollection(collection);
                String modelName = (config != null && config.getModel() != null)
                        ? config.getModel()
                        : embeddingProperties.getModel();
                collectionsByModel.computeIfAbsent(modelName, k -> new ArrayList<>()).add(collection);
            }

            int totalIndexed = 0;
            int embeddedChunkCount = 0;
            List<String> failedCollections = new ArrayList<>();
            List<String> embeddingErrors = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : collectionsByModel.entrySet()) {
                String modelName = entry.getKey();
                List<DocumentChunk> modelChunks = new ArrayList<>();
                List<float[]> modelVectors = new ArrayList<>();

                // Embed chunk-by-chunk so one failing chunk (e.g. one that overflows the
                // embedding model context) cannot silently abort the whole document.
                for (DocumentChunk chunkObj : chunks) {
                    try {
                        modelVectors.add(embeddingClient.embedWithModel(chunkObj.getContent(), modelName));
                        modelChunks.add(chunkObj);
                    } catch (Exception e) {
                        embeddingErrors.add("chunk " + chunkObj.getSequenceNumber() + " with model '"
                                + modelName + "': " + e.getMessage());
                        log.warn("Embedding failed for chunk {} of document {} with model {}: {}",
                                chunkObj.getSequenceNumber(), request.getDocumentId(), modelName, e.getMessage());
                    }
                }

                if (modelChunks.isEmpty()) {
                    // Nothing embeddable for this model: every collection using it failed.
                    failedCollections.addAll(entry.getValue());
                    continue;
                }
                embeddedChunkCount = Math.max(embeddedChunkCount, modelChunks.size());

                for (String collection : entry.getValue()) {
                    try {
                        // Make re-ingest idempotent: drop prior chunks for this document first.
                        int removed = qdrantRepository.deleteDocument(request.getDocumentId(), collection);
                        if (removed > 0) {
                            log.info("Re-ingest cleanup: removed {} stale chunks for document {} from {}",
                                    removed, request.getDocumentId(), collection);
                        }
                        int indexed = qdrantRepository.batchIndex(
                                collection,
                                request.getDocumentId(),
                                modelChunks,
                                modelVectors,
                                embeddingProperties.getDimension(),
                                embeddingProperties.getBatchSize()
                        );
                        totalIndexed += indexed;
                    } catch (Exception e) {
                        log.error("Indexing failed for collection {}: {}", collection, e.getMessage());
                        failedCollections.add(collection);
                    }
                }
            }

            if (embeddedChunkCount == 0) {
                throw new RuntimeException("No chunks could be embedded for document "
                        + request.getDocumentId() + ": "
                        + (embeddingErrors.isEmpty()
                                ? "embedding service returned no vectors"
                                : String.join("; ", embeddingErrors)));
            }

            IngestDocumentResponse.Builder responseBuilder = IngestDocumentResponse.newBuilder()
                    .setSuccess(failedCollections.isEmpty() && embeddingErrors.isEmpty())
                    .setChunksIndexed(totalIndexed)
                    .addAllIndexedCollections(targetCollections)
                    .addAllFailedCollections(failedCollections);
            if (!embeddingErrors.isEmpty()) {
                responseBuilder.setErrorMessage(
                        String.join("; ", embeddingErrors)
                        + " (" + embeddedChunkCount + " of " + chunks.size() + " chunks embedded)");
            }
            IngestDocumentResponse response = responseBuilder.build();

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
                                                boolean fallbackUsed, int collectionsSearched,
                                                List<String> failedCollections, boolean success,
                                                String errorMessage) {
        HybridSearchResponse.Builder builder = HybridSearchResponse.newBuilder()
                .setSuccess(success)
                .setFallbackUsed(fallbackUsed)
                .setCollectionsSearched(collectionsSearched);

        if (errorMessage != null) {
            builder.setErrorMessage(errorMessage);
        }

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
            
            // Reject anything that is not a supported distance metric instead of
            // silently falling back to Cosine.
            Collections.Distance distance = resolveDistance(request.getDistance(), Collections.Distance.Cosine);
            
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
            
            // Collections whose query config could not be read: the listing still
            // returns them, but the response says which ones are incomplete.
            List<String> configFailures = new ArrayList<>();
            
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
                
                CollectionQueryConfig queryConfig = fetchCollectionQueryConfig(name);
                if (!queryConfig.success()) {
                    log.warn("Could not read config for collection {}: {}", name, queryConfig.errorMessage());
                    configFailures.add(name + ": " + queryConfig.errorMessage());
                }
                int vectorDimension = queryConfig.vectorDimension();
                String distanceMetric = queryConfig.distanceMetric();
                
                String embeddingModel = embeddingProperties.getModel();
                var collectionEmbeddingConfig = embeddingProperties.getConfigForCollection(name);
                if (collectionEmbeddingConfig != null && collectionEmbeddingConfig.getModel() != null) {
                    embeddingModel = collectionEmbeddingConfig.getModel();
                }
                
                CollectionInfo info = CollectionInfo.newBuilder()
                        .setName(name)
                        .setPointsCount((int) Math.min(Integer.MAX_VALUE, collectionInfo.getPointsCount()))
                        .setVectorDimension(vectorDimension)
                        .setDistanceMetric(distanceMetric)
                        .setStatus(collectionInfo.getStatus().name())
                        .setCacheSize(cacheSize)
                        .setTotalDocuments(totalDocuments)
                        .setEmbeddingModel(embeddingModel)
                        .build();
                responseBuilder.addCollections(info);
            }
            
            if (!configFailures.isEmpty()) {
                responseBuilder.setErrorMessage("Could not read query config for "
                        + configFailures.size() + " of " + collectionNames.size() + " collections: "
                        + String.join("; ", configFailures));
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
            
            CollectionQueryConfig queryConfig = fetchCollectionQueryConfig(collectionName);
            if (!queryConfig.success()) {
                log.error("Could not read config for collection {}: {}", collectionName, queryConfig.errorMessage());
                responseObserver.onNext(GetCollectionInfoResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage(queryConfig.errorMessage())
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            CollectionInfo info = CollectionInfo.newBuilder()
                    .setName(collectionName)
                    .setPointsCount((int) Math.min(Integer.MAX_VALUE, collectionInfo.getPointsCount()))
                    .setVectorDimension(queryConfig.vectorDimension())
                    .setDistanceMetric(queryConfig.distanceMetric())
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
            int limit = clampLimit(request.getLimit(), 100, 1000);
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
            
            var scan = qdrantRepository.rebuildDocumentCache(collectionName);
            
            com.mcp.qdrant.proto.RebuildDocumentCacheResponse response = 
                    com.mcp.qdrant.proto.RebuildDocumentCacheResponse.newBuilder()
                            .setSuccess(true)
                            .setTotalDocuments(scan.totalDocuments())
                            .setCollectionsScanned(scan.collectionsScanned())
                            .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Document cache rebuilt. Found {} documents across {} collections",
                    scan.totalDocuments(), scan.collectionsScanned());

        } catch (Exception e) {
            log.error("Failed to rebuild document cache: {}", e.getMessage(), e);
            responseObserver.onNext(com.mcp.qdrant.proto.RebuildDocumentCacheResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    private record CollectionQueryConfig(boolean success, String errorMessage, int vectorDimension,
                                          String distanceMetric) {}

    private CollectionQueryConfig queryConfigFailure(String message) {
        return new CollectionQueryConfig(false, message, 0, "");
    }

    private CollectionQueryConfig fetchCollectionQueryConfig(String collectionName) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> collInfo = webClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofMillis(qdrantProperties.getTimeoutMs()))
                    .block();

            if (collInfo == null) {
                return queryConfigFailure("No collection config returned for " + collectionName);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) collInfo.get("result");
            if (result == null) {
                return queryConfigFailure("Collection config response has no result for " + collectionName);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) result.get("config");
            if (config == null) {
                return queryConfigFailure("Collection config has no config section for " + collectionName);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) config.get("params");
            if (params == null) {
                return queryConfigFailure("Collection config has no params section for " + collectionName);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> vectors = (Map<String, Object>) params.get("vectors");
            if (vectors == null) {
                return queryConfigFailure("Collection config has no vectors section for " + collectionName);
            }

            int dimension = 0;
            Object sizeObj = vectors.get("size");
            if (sizeObj instanceof Integer) {
                dimension = (Integer) sizeObj;
            } else if (sizeObj instanceof Long) {
                dimension = ((Long) sizeObj).intValue();
            } else if (sizeObj instanceof Number) {
                dimension = ((Number) sizeObj).intValue();
            }
            if (dimension <= 0) {
                return queryConfigFailure("Collection config has no vector size for " + collectionName);
            }

            Object distObj = vectors.get("distance");
            if (!(distObj instanceof String) || ((String) distObj).isEmpty()) {
                return queryConfigFailure("Collection config has no distance metric for " + collectionName);
            }

            return new CollectionQueryConfig(true, null, dimension, (String) distObj);

        } catch (Exception e) {
            log.warn("Could not fetch collection config for {}: {}", collectionName, e.getMessage());
            return queryConfigFailure("Could not fetch collection config for " + collectionName
                    + ": " + e.getMessage());
        }
    }
}

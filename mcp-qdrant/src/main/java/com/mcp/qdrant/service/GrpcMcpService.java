package com.mcp.qdrant.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mcp.qdrant.chunker.TextChunker;
import com.mcp.qdrant.client.EmbeddingServiceClient;
import com.mcp.qdrant.config.EmbeddingProperties;
import com.mcp.qdrant.config.QdrantProperties;
import com.mcp.qdrant.model.DocumentChunk;
import com.mcp.qdrant.model.SearchResult;
import com.mcp.qdrant.proto.CollectionInfo;
import com.mcp.qdrant.proto.CreateCollectionRequest;
import com.mcp.qdrant.proto.CreateCollectionResponse;
import com.mcp.qdrant.proto.DeleteCollectionRequest;
import com.mcp.qdrant.proto.DeleteCollectionResponse;
import com.mcp.qdrant.proto.DocumentSource;
import com.mcp.qdrant.proto.GetCollectionInfoRequest;
import com.mcp.qdrant.proto.GetCollectionInfoResponse;
import com.mcp.qdrant.proto.HybridSearchRequest;
import com.mcp.qdrant.proto.HybridSearchResponse;
import com.mcp.qdrant.proto.IngestDocumentRequest;
import com.mcp.qdrant.proto.IngestDocumentResponse;
import com.mcp.qdrant.proto.ListCollectionsRequest;
import com.mcp.qdrant.proto.ListCollectionsResponse;
import com.mcp.qdrant.proto.McpQdrantServiceGrpc;
import com.mcp.qdrant.proto.SearchResult.Builder;
import com.mcp.qdrant.repository.QdrantRepository;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
    
    public GrpcMcpService(EmbeddingServiceClient embeddingClient, QdrantRepository qdrantRepository,
                          QdrantProperties qdrantProperties, EmbeddingProperties embeddingProperties,
                          TextChunker textChunker, SummarizationService summarizationService) {
        this.embeddingClient = embeddingClient;
        this.qdrantRepository = qdrantRepository;
        this.qdrantProperties = qdrantProperties;
        this.embeddingProperties = embeddingProperties;
        this.textChunker = textChunker;
        this.summarizationService = summarizationService;
    }

    @Override
    public void hybridSearch(HybridSearchRequest request, StreamObserver<HybridSearchResponse> responseObserver) {
        try {
            List<SearchResult> allResults = new ArrayList<>();
            int collectionsSearched = 0;
            List<String> failedCollections = new ArrayList<>();

            // Handle default limit (num_results) - default to 5 if not specified
            int limit = request.hasLimit() && request.getLimit() > 0 ? request.getLimit() : 5;

            // Determine if summarization should be used
            // Only summarize if: explicitly requested AND model is configured/available
            boolean modelAvailable = embeddingProperties.getModel() != null 
                    && !embeddingProperties.getModel().isEmpty()
                    && !"null".equals(embeddingProperties.getModel());
            boolean summarizeRequested = request.hasSummarize() && request.getSummarize();
            boolean shouldSummarize = summarizeRequested && modelAvailable;

            float[] queryVector = embeddingClient.embed(request.getQueryText());

            List<String> collectionsToSearch = getCollectionsToSearch();
            
            for (String collection : collectionsToSearch) {
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
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL.withDescription("Search failed: " + e.getMessage())
            ));
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
                CollectionInfo info = CollectionInfo.newBuilder().setName(name).build();
                responseBuilder.addCollections(info);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("Listed {} collections", collectionNames.size());

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
}

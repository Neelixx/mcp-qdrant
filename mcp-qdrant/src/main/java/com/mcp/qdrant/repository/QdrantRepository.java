package com.mcp.qdrant.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.mcp.qdrant.config.CacheConfig;
import com.mcp.qdrant.config.QdrantProperties;
import com.mcp.qdrant.model.DocumentChunk;
import com.mcp.qdrant.model.SearchResult;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;

@Repository
public class QdrantRepository {

    private static final Logger log = LoggerFactory.getLogger(QdrantRepository.class);
    
    private final QdrantClient qdrantClient;
    private final QdrantProperties properties;
    private final CacheConfig cacheConfig;
    
    public QdrantRepository(QdrantClient qdrantClient, QdrantProperties properties, CacheConfig cacheConfig) {
        this.qdrantClient = qdrantClient;
        this.properties = properties;
        this.cacheConfig = cacheConfig;
    }

    public List<SearchResult> search(String collectionName, float[] vector, int limit, Map<String, String> filters) {
        try {
            // Handle "all" as special case - search all collections
            if ("all".equals(collectionName)) {
                return searchAllCollections(vector, limit, filters);
            }
            
            List<Points.ScoredPoint> results = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .addAllVector(toFloatList(vector))
                            .setLimit(limit)
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                            .build()
            ).get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);

            return results.stream()
                    .map(point -> SearchResult.builder()
                            .id(point.getId().getUuid())
                            .score(point.getScore())
                            .payload(extractContent(point.getPayloadMap()))
                            .collection(collectionName)
                            .metadata(extractMetadata(point.getPayloadMap()))
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Qdrant search failed for collection {}: {}", collectionName, e.getMessage());
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }
    
    private List<SearchResult> searchAllCollections(float[] vector, int limit, Map<String, String> filters) {
        try {
            List<String> allCollections = qdrantClient.listCollectionsAsync()
                    .get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            
            List<SearchResult> allResults = new ArrayList<>();
            int perCollectionLimit = Math.max(limit / allCollections.size(), 10);
            
            for (String collection : allCollections) {
                try {
                    List<Points.ScoredPoint> results = qdrantClient.searchAsync(
                            Points.SearchPoints.newBuilder()
                                    .setCollectionName(collection)
                                    .addAllVector(toFloatList(vector))
                                    .setLimit(perCollectionLimit)
                                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                                    .build()
                    ).get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);

                    for (Points.ScoredPoint point : results) {
                        allResults.add(SearchResult.builder()
                                .id(point.getId().getUuid())
                                .score(point.getScore())
                                .payload(extractContent(point.getPayloadMap()))
                                .collection(collection)
                                .metadata(extractMetadata(point.getPayloadMap()))
                                .build());
                    }
                } catch (Exception e) {
                    log.warn("Search failed for collection {}: {}", collection, e.getMessage());
                }
            }
            
            // Sort by score and limit results
            return allResults.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(limit)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Failed to search all collections: {}", e.getMessage());
            throw new RuntimeException("Search all collections failed: " + e.getMessage(), e);
        }
    }

    @CacheEvict(value = {"documentListCache", "documentInfoCache"}, allEntries = true)
    public int batchIndex(String collectionName, String documentId, List<DocumentChunk> chunks, List<float[]> vectors, int dimension) {
        // Add at start of batchIndex() method (line 60):
        if (chunks.size() != vectors.size()) {
            throw new IllegalArgumentException("Chunks and vectors must have same size: chunks=" 
                + chunks.size() + ", vectors=" + vectors.size());
        }
        try {
            // Ensure collection exists before indexing
            ensureCollectionExists(collectionName, dimension);
            
            List<Points.PointStruct> points = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                float[] vector = vectors.get(i);

                Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
                payload.put("document_id", jsonValue(documentId));
                payload.put("chunk_id", jsonValue(chunk.getChunkId()));
                payload.put("content", jsonValue(chunk.getContent()));
                payload.put("sequence", jsonValue(String.valueOf(chunk.getSequenceNumber())));

                if (chunk.getMetadata() != null) {
                    chunk.getMetadata().forEach((key, value) ->
                            payload.put(key, jsonValue(value)));
                }

                List<Float> floatList = toFloatList(vector);
                points.add(Points.PointStruct.newBuilder()
                        .setId(Points.PointId.newBuilder().setUuid(chunk.getChunkId()).build())
                        .setVectors(Points.Vectors.newBuilder()
                                .setVector(Points.Vector.newBuilder().addAllData(floatList).build())
                                .build())
                        .putAllPayload(payload)
                        .build());
            }

            qdrantClient.upsertAsync(
                    Points.UpsertPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .addAllPoints(points)
                            .build()
            ).get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);

            log.info("Indexed {} chunks to collection {}", chunks.size(), collectionName);
            return chunks.size();

        } catch (Exception e) {
            log.error("Batch indexing failed for collection {}: {}", collectionName, e.getMessage());
            throw new RuntimeException("Indexing failed: " + e.getMessage(), e);
        }
    }

    public void ensureCollectionExists(String collectionName, int dimension) {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);

            if (!exists) {
                log.info("Creating collection: {}", collectionName);
                Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                        .setDistance(Collections.Distance.Cosine)
                        .setSize(dimension)
                        .build();
                qdrantClient.createCollectionAsync(collectionName, vectorParams).get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
                log.info("Collection {} created successfully", collectionName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure collection exists {}: {}", collectionName, e.getMessage());
            throw new RuntimeException("Collection creation failed: " + e.getMessage(), e);
        }
    }

    private Map<String, String> extractMetadata(Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload) {
        Map<String, String> metadata = new HashMap<>();
        payload.forEach((key, value) -> {
            if (value.hasStringValue()) {
                metadata.put(key, value.getStringValue());
            }
        });
        return metadata;
    }

    private String extractContent(Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload) {
        io.qdrant.client.grpc.JsonWithInt.Value contentValue = payload.get("content");
        if (contentValue != null && contentValue.hasStringValue()) {
            return contentValue.getStringValue();
        }
        return "";
    }

    private io.qdrant.client.grpc.JsonWithInt.Value jsonValue(String value) {
        return io.qdrant.client.grpc.JsonWithInt.Value.newBuilder()
                .setStringValue(value)
                .build();
    }
    
    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    public QdrantClient getQdrantClient() {
        return qdrantClient;
    }

    /**
     * Lists all unique documents in a collection or across all collections.
     * Documents are identified by their document_id in chunk payloads.
     *
     * @param collectionName specific collection to search, or null/empty for all collections
     * @param limit maximum number of documents to return
     * @param offset number of documents to skip (for pagination)
     * @return sorted list of DocumentInfo containing collection and chunk count
     */
@Cacheable(value = "documentListCache", key = "#collectionName + '-' + #limit + '-' + #offset")
    public List<DocumentInfo> listDocuments(String collectionName, int limit, int offset) {
        Map<String, DocumentInfo> documents = new HashMap<>();
        
        try {
            List<String> collectionsToSearch;
            if (collectionName != null && !collectionName.isEmpty()) {
                collectionsToSearch = List.of(collectionName);
            } else {
                collectionsToSearch = qdrantClient.listCollectionsAsync()
                        .get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            }
            
            for (String collection : collectionsToSearch) {
                try {
                    // Scroll through all points using offset pagination
                    Points.ScrollPoints.Builder scrollBuilder = Points.ScrollPoints.newBuilder()
                            .setCollectionName(collection)
                            .setLimit(100) // Batch size for scrolling
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build());
                    
                    Points.PointId nextOffset = null;
                    boolean hasMore = true;
                    
                    while (hasMore) {
                        if (nextOffset != null) {
                            scrollBuilder.setOffset(nextOffset);
                        }
                        
                        Points.ScrollResponse response = qdrantClient.scrollAsync(scrollBuilder.build())
                                .get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
                        
                        for (Points.RetrievedPoint point : response.getResultList()) {
                            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = point.getPayloadMap();
                            io.qdrant.client.grpc.JsonWithInt.Value docIdValue = payload.get("document_id");
                            
                            if (docIdValue != null && docIdValue.hasStringValue()) {
                                String docId = docIdValue.getStringValue();
                                
                                DocumentInfo info = documents.computeIfAbsent(docId, 
                                    k -> new DocumentInfo(docId, collection, 0));
                                info.incrementChunkCount();
                            }
                        }
                        
                        hasMore = response.hasNextPageOffset();
                        if (hasMore) {
                            nextOffset = response.getNextPageOffset();
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to scroll collection {}: {}", collection, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to list documents: {}", e.getMessage());
            throw new RuntimeException("Failed to list documents: " + e.getMessage(), e);
        }
        
        // Sort by document_id and apply offset/limit
        return documents.values().stream()
                .sorted((a, b) -> a.getDocumentId().compareTo(b.getDocumentId()))
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Deletes all chunks for a specific document from Qdrant.
     *
     * @param documentId the document ID to delete
     * @param collectionName specific collection, or null/empty to search all
     * @return number of chunks deleted
     */
    @CacheEvict(value = {"documentListCache", "documentInfoCache"}, allEntries = true)
    public int deleteDocument(String documentId, String collectionName) {
        int deletedCount = 0;
        String targetCollection = null;
        
        try {
            List<String> collectionsToSearch;
            if (collectionName != null && !collectionName.isEmpty()) {
                collectionsToSearch = List.of(collectionName);
            } else {
                collectionsToSearch = qdrantClient.listCollectionsAsync()
                        .get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            }
            
            for (String collection : collectionsToSearch) {
                try {
                    // Find all points with this document_id
                    List<Points.PointId> pointsToDelete = new ArrayList<>();
                    
                    Points.ScrollPoints.Builder scrollBuilder = Points.ScrollPoints.newBuilder()
                            .setCollectionName(collection)
                            .setLimit(1000)
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build());
                    
                    Points.PointId nextOffset = null;
                    boolean hasMore = true;
                    
                    while (hasMore) {
                        if (nextOffset != null) {
                            scrollBuilder.setOffset(nextOffset);
                        }
                        
                        Points.ScrollResponse response = qdrantClient.scrollAsync(scrollBuilder.build())
                                .get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
                        
                        for (Points.RetrievedPoint point : response.getResultList()) {
                            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = point.getPayloadMap();
                            io.qdrant.client.grpc.JsonWithInt.Value docIdValue = payload.get("document_id");
                            
                            if (docIdValue != null && docIdValue.hasStringValue() 
                                    && docIdValue.getStringValue().equals(documentId)) {
                                pointsToDelete.add(point.getId());
                            }
                        }
                        
                        hasMore = response.hasNextPageOffset();
                        if (hasMore) {
                            nextOffset = response.getNextPageOffset();
                        }
                    }
                    
                    // Delete points using payload filter on document_id
                    try {
                        Points.Filter filter = Points.Filter.newBuilder()
                                .addMust(
                                        Points.Condition.newBuilder()
                                                .setField(Points.FieldCondition.newBuilder()
                                                        .setKey("document_id")
                                                        .setMatch(Points.Match.newBuilder()
                                                                .setText(documentId)
                                                                .build()
                                                        )
                                                        .build()
                                                )
                                                .build()
                                )
                                .build();
                        
                        Points.PointsSelector selector = Points.PointsSelector.newBuilder()
                                .setFilter(filter)
                                .build();
                        
                        qdrantClient.deleteAsync(
                                Points.DeletePoints.newBuilder()
                                        .setCollectionName(collection)
                                        .setPoints(selector)
                                        .build()
                        ).get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
                        
                        deletedCount += pointsToDelete.size();
                        targetCollection = collection;
                        log.info("Deleted {} chunks for document {} from collection {}", 
                                pointsToDelete.size(), documentId, collection);
                    } catch (Exception e) {
                        log.error("Failed to delete points for document {}: {}", documentId, e.getMessage());
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to delete document {} from collection {}: {}", 
                            documentId, collection, e.getMessage());
                }
            }
            
            if (deletedCount == 0) {
                log.warn("Document {} not found in any collection", documentId);
            }
            
            return deletedCount;
            
        } catch (Exception e) {
            log.error("Failed to delete document {}: {}", documentId, e.getMessage());
            throw new RuntimeException("Failed to delete document: " + e.getMessage(), e);
        }
    }

    /**
     * Gets information about a specific document.
     *
     * @param documentId the document ID to look up
     * @param collectionName specific collection, or null/empty to search all
     * @return DocumentInfo if found, null otherwise
     */
    @Cacheable(value = "documentInfoCache", key = "#documentId + '-' + (#collectionName ?: 'all')")
    public DocumentInfo getDocumentInfo(String documentId, String collectionName) {
        try {
            List<String> collectionsToSearch;
            if (collectionName != null && !collectionName.isEmpty()) {
                collectionsToSearch = List.of(collectionName);
            } else {
                collectionsToSearch = qdrantClient.listCollectionsAsync()
                        .get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            }
            
            for (String collection : collectionsToSearch) {
                try {
                    Points.ScrollPoints.Builder scrollBuilder = Points.ScrollPoints.newBuilder()
                            .setCollectionName(collection)
                            .setLimit(1000)
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build());
                    
                    Points.PointId nextOffset = null;
                    boolean hasMore = true;
                    int chunkCount = 0;
                    
                    while (hasMore) {
                        if (nextOffset != null) {
                            scrollBuilder.setOffset(nextOffset);
                        }
                        
                        Points.ScrollResponse response = qdrantClient.scrollAsync(scrollBuilder.build())
                                .get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
                        
                        for (Points.RetrievedPoint point : response.getResultList()) {
                            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = point.getPayloadMap();
                            io.qdrant.client.grpc.JsonWithInt.Value docIdValue = payload.get("document_id");
                            
                            if (docIdValue != null && docIdValue.hasStringValue() 
                                    && docIdValue.getStringValue().equals(documentId)) {
                                chunkCount++;
                            }
                        }
                        
                        hasMore = response.hasNextPageOffset();
                        if (hasMore) {
                            nextOffset = response.getNextPageOffset();
                        }
                    }
                    
                    if (chunkCount > 0) {
                        return new DocumentInfo(documentId, collection, chunkCount);
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to get document info for {} from collection {}: {}", 
                            documentId, collection, e.getMessage());
                }
            }
            
            return null; // Document not found
            
        } catch (Exception e) {
            log.error("Failed to get document info for {}: {}", documentId, e.getMessage());
            throw new RuntimeException("Failed to get document info: " + e.getMessage(), e);
        }
    }

    /**
     * Rebuilds the document cache by scanning all collections.
     * This evicts all existing cache entries and recalculates cache size
     * based on document count: min(1000, totalDocuments).
     *
     * @param collectionName specific collection, or null/empty for all
     * @return total number of unique documents found
     */
    @CacheEvict(value = {"documentListCache", "documentInfoCache"}, allEntries = true)
    public int rebuildDocumentCache(String collectionName) {
        log.info("Rebuilding document cache for collection: {}", 
                collectionName != null && !collectionName.isEmpty() ? collectionName : "all");
        
        try {
            // Get all collections to process
            List<String> collectionsToProcess;
            if (collectionName != null && !collectionName.isEmpty()) {
                collectionsToProcess = List.of(collectionName);
            } else {
                collectionsToProcess = qdrantClient.listCollectionsAsync()
                        .get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            }
            
            // Get all documents for the specified collections
            List<DocumentInfo> allDocuments = listDocuments(collectionName, Integer.MAX_VALUE, 0);
            
            // Count documents per collection and update cache configuration
            Map<String, Integer> docsPerCollection = new HashMap<>();
            for (DocumentInfo doc : allDocuments) {
                docsPerCollection.merge(doc.getCollection(), 1, Integer::sum);
            }
            
            // Update cache size for each collection: max(1000, totalDocuments)
            for (String collection : collectionsToProcess) {
                int docCount = docsPerCollection.getOrDefault(collection, 0);
                cacheConfig.updateCacheSizeForCollection(collection, docCount);
                log.info("Updated cache size for collection {}: {} documents, cache size: {}", 
                        collection, docCount, Math.max(1000, docCount));
            }
            
            int collectionsScanned = collectionsToProcess.size();
            
            log.info("Document cache rebuilt. Found {} documents across {} collections", 
                    allDocuments.size(), collectionsScanned);
            
            return allDocuments.size();
            
        } catch (Exception e) {
            log.error("Failed to rebuild document cache: {}", e.getMessage());
            throw new RuntimeException("Failed to rebuild document cache: " + e.getMessage(), e);
        }
    }

    /**
     * Simple DTO for document information during listing
     */
    public static class DocumentInfo {
        private final String documentId;
        private final String collection;
        private int chunkCount;

        public DocumentInfo(String documentId, String collection, int chunkCount) {
            this.documentId = documentId;
            this.collection = collection;
            this.chunkCount = chunkCount;
        }

        public String getDocumentId() { return documentId; }
        public String getCollection() { return collection; }
        public int getChunkCount() { return chunkCount; }
        public void incrementChunkCount() { this.chunkCount++; }
    }
}

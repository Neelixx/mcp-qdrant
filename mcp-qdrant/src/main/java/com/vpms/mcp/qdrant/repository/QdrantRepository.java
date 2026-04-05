package com.vpms.mcp.qdrant.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.vpms.mcp.qdrant.model.DocumentChunk;
import com.vpms.mcp.qdrant.model.SearchResult;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;

@Repository
public class QdrantRepository {

    private static final Logger log = LoggerFactory.getLogger(QdrantRepository.class);
    
    private final QdrantClient qdrantClient;
    
    public QdrantRepository(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
    }

    public List<SearchResult> search(String collectionName, float[] vector, int limit, Map<String, String> filters) {
        try {
            List<Points.ScoredPoint> results = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .addAllVector(toFloatList(vector))
                            .setLimit(limit)
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                            .build()
            ).get(5, TimeUnit.SECONDS);

            return results.stream()
                    .map(point -> SearchResult.builder()
                            .id(point.getId().getUuid())
                            .score(point.getScore())
                            .payload(point.getPayload().toString())
                            .collection(collectionName)
                            .metadata(extractMetadata(point.getPayloadMap()))
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Qdrant search failed for collection {}: {}", collectionName, e.getMessage());
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    public int batchIndex(String collectionName, String documentId, List<DocumentChunk> chunks, List<float[]> vectors, int dimension) {
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
            ).get(10, TimeUnit.SECONDS);

            log.info("Indexed {} chunks to collection {}", chunks.size(), collectionName);
            return chunks.size();

        } catch (Exception e) {
            log.error("Batch indexing failed for collection {}: {}", collectionName, e.getMessage());
            throw new RuntimeException("Indexing failed: " + e.getMessage(), e);
        }
    }

    public void ensureCollectionExists(String collectionName, int dimension) {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get(5, TimeUnit.SECONDS);

            if (!exists) {
                log.info("Creating collection: {}", collectionName);
                Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                        .setDistance(Collections.Distance.Cosine)
                        .setSize(dimension)
                        .build();
                qdrantClient.createCollectionAsync(collectionName, vectorParams).get(10, TimeUnit.SECONDS);
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
}

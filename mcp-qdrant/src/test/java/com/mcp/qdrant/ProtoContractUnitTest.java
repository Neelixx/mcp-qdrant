package com.mcp.qdrant;

import com.mcp.qdrant.proto.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for all proto message types and their builders.
 * Verifies that all RPC contracts are correctly defined.
 */
public class ProtoContractUnitTest {

    @Test
    void testAllRequestTypes_Instantiable() {
        // Verify all request types can be built
        assertNotNull(CreateCollectionRequest.getDefaultInstance());
        assertNotNull(DeleteCollectionRequest.getDefaultInstance());
        assertNotNull(ListCollectionsRequest.getDefaultInstance());
        assertNotNull(GetCollectionInfoRequest.getDefaultInstance());
        assertNotNull(IngestDocumentRequest.getDefaultInstance());
        assertNotNull(HybridSearchRequest.getDefaultInstance());
        assertNotNull(BackupCollectionRequest.getDefaultInstance());
        assertNotNull(RestoreCollectionRequest.getDefaultInstance());
    }

    @Test
    void testAllResponseTypes_Instantiable() {
        // Verify all response types can be built
        assertNotNull(CreateCollectionResponse.getDefaultInstance());
        assertNotNull(DeleteCollectionResponse.getDefaultInstance());
        assertNotNull(ListCollectionsResponse.getDefaultInstance());
        assertNotNull(GetCollectionInfoResponse.getDefaultInstance());
        assertNotNull(IngestDocumentResponse.getDefaultInstance());
        assertNotNull(HybridSearchResponse.getDefaultInstance());
        assertNotNull(BackupCollectionResponse.getDefaultInstance());
        assertNotNull(RestoreCollectionResponse.getDefaultInstance());
    }

    @Test
    void testComplexMessage_DefaultInstances() {
        assertNotNull(CollectionInfo.getDefaultInstance());
        assertNotNull(SearchResult.getDefaultInstance());
        assertNotNull(DocumentSource.getDefaultInstance());
        assertNotNull(ChunkingConfig.getDefaultInstance());
    }

    @Test
    void testCollectionInfo_BuilderChain() {
        CollectionInfo info = CollectionInfo.newBuilder()
                .setName("chain-test")
                .setPointsCount(1000)
                .setVectorDimension(768)
                .setDistanceMetric("Cosine")
                .setStatus("green")
                .build();

        assertAll("builder chain",
                () -> assertEquals("chain-test", info.getName()),
                () -> assertEquals(1000, info.getPointsCount()),
                () -> assertEquals(768, info.getVectorDimension()),
                () -> assertEquals("Cosine", info.getDistanceMetric()),
                () -> assertEquals("green", info.getStatus())
        );
    }

    @Test
    void testSearchResult_Serialization() {
        SearchResult original = SearchResult.newBuilder()
                .setId("test-123")
                .setScore(0.85f)
                .setPayload("{\"key\":\"value\"}")
                .setCollection("test-collection")
                .putMetadata("meta", "data")
                .build();

        byte[] serialized = original.toByteArray();
        assertTrue(serialized.length > 0, "Should serialize to bytes");
    }

    @Test
    void testCreateCollectionRequest_DefaultValues() {
        CreateCollectionRequest request = CreateCollectionRequest.newBuilder()
                .setCollectionName("test")
                .build();

        // Optional fields should have default values
        assertEquals(0, request.getDimension());
        assertTrue(request.getDistance().isEmpty());
    }

    @Test
    void testIngestDocumentRequest_DefaultChunking() {
        IngestDocumentRequest request = IngestDocumentRequest.newBuilder()
                .setDocumentId("doc-1")
                .setContent("content")
                .addTargetCollections("col-1")
                .build();

        // ChunkingConfig should be empty by default
        assertFalse(request.hasChunkingConfig());
    }

    @Test
    void testHybridSearchRequest_DefaultLimit() {
        HybridSearchRequest request = HybridSearchRequest.newBuilder()
                .setQueryText("test")
                .build();

        assertEquals(0, request.getLimit());
        assertFalse(request.getSummarize());
        assertTrue(request.getFiltersMap().isEmpty());
    }
}

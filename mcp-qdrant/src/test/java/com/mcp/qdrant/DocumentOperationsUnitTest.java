package com.mcp.qdrant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.mcp.qdrant.proto.ChunkingConfig;
import com.mcp.qdrant.proto.DocumentSource;
import com.mcp.qdrant.proto.HybridSearchRequest;
import com.mcp.qdrant.proto.HybridSearchResponse;
import com.mcp.qdrant.proto.IngestDocumentRequest;
import com.mcp.qdrant.proto.IngestDocumentResponse;
import com.mcp.qdrant.proto.SearchResult;

/**
 * Unit tests for document operations (ingest and search).
 * Tests verify request/response message construction.
 */
public class DocumentOperationsUnitTest {

    @Test
    void testIngestDocumentRequest_Builder() {
        IngestDocumentRequest request = IngestDocumentRequest.newBuilder()
                .setDocumentId("doc-123")
                .setContent("This is test content about machine learning and AI.")
                .addTargetCollections("collection-a")
                .addTargetCollections("collection-b")
                .putMetadata("source", "test")
                .putMetadata("author", "test-author")
                .setChunkingConfig(
                        ChunkingConfig.newBuilder()
                                .setChunkSize(100)
                                .setChunkOverlap(20)
                                .setSeparator(".")
                                .build()
                )
                .build();

        assertEquals("doc-123", request.getDocumentId());
        assertEquals("This is test content about machine learning and AI.", request.getContent());
        assertEquals(2, request.getTargetCollectionsCount());
        assertTrue(request.getTargetCollectionsList().contains("collection-a"));
        assertTrue(request.getTargetCollectionsList().contains("collection-b"));
        assertEquals("test", request.getMetadataMap().get("source"));
        assertEquals(100, request.getChunkingConfig().getChunkSize());
        assertEquals(20, request.getChunkingConfig().getChunkOverlap());
    }

    @Test
    void testIngestDocumentResponse_Builder() {
        IngestDocumentResponse response = IngestDocumentResponse.newBuilder()
                .setSuccess(true)
                .setChunksIndexed(5)
                .addIndexedCollections("collection-a")
                .addIndexedCollections("collection-b")
                .build();

        assertTrue(response.getSuccess());
        assertEquals(5, response.getChunksIndexed());
        assertEquals(2, response.getIndexedCollectionsCount());
    }

    @Test
    void testIngestDocumentResponse_Error() {
        IngestDocumentResponse response = IngestDocumentResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage("Collection not found")
                .addFailedCollections("non-existent-collection")
                .build();

        assertFalse(response.getSuccess());
        assertEquals("Collection not found", response.getErrorMessage());
        assertEquals(1, response.getFailedCollectionsCount());
    }

    @Test
    void testHybridSearch_WithFilters() {
        String query = "machine learning";
        int limit = 10;

        HybridSearchRequest request = HybridSearchRequest.newBuilder()
                .setQueryText(query)
                .setLimit(limit)
                .putFilters("source", "documentation")
                .putFilters("language", "en")
                .setSummarize(true)
                .build();

        assertEquals(query, request.getQueryText());
        assertEquals(limit, request.getLimit());
        assertEquals(2, request.getFiltersCount());
        assertEquals("documentation", request.getFiltersMap().get("source"));
        assertTrue(request.getSummarize());
    }

    @Test
    void testHybridSearchResponse_Builder() {
        SearchResult result1 = SearchResult.newBuilder()
                .setId("point-1")
                .setScore(0.95f)
                .setPayload("{\"text\":\"result 1\"}")
                .setCollection("collection-a")
                .putMetadata("doc_id", "doc-1")
                .build();

        SearchResult result2 = SearchResult.newBuilder()
                .setId("point-2")
                .setScore(0.87f)
                .setPayload("{\"text\":\"result 2\"}")
                .setCollection("collection-b")
                .build();

        DocumentSource source = DocumentSource.newBuilder()
                .setDocumentId("doc-1")
                .setCollection("collection-a")
                .setChunkCount(3)
                .build();

        HybridSearchResponse response = HybridSearchResponse.newBuilder()
                .addResults(result1)
                .addResults(result2)
                .setSummary("Summary of results")
                .setFallbackUsed(false)
                .setCollectionsSearched(2)
                .addSources(source)
                .build();

        assertEquals(2, response.getResultsCount());
        assertEquals(0.95f, response.getResults(0).getScore(), 0.001);
        assertEquals("Summary of results", response.getSummary());
        assertFalse(response.getFallbackUsed());
        assertEquals(2, response.getCollectionsSearched());
    }

    @Test
    void testSearchResult_Metadata() {
        SearchResult result = SearchResult.newBuilder()
                .setId("test-id")
                .setScore(0.75f)
                .putMetadata("key1", "value1")
                .putMetadata("key2", "value2")
                .build();

        assertEquals("test-id", result.getId());
        assertEquals(2, result.getMetadataCount());
        assertEquals("value1", result.getMetadataMap().get("key1"));
    }
}

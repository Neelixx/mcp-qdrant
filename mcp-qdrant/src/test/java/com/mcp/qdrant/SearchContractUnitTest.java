package com.mcp.qdrant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.mcp.qdrant.repository.QdrantRepository;
import com.mcp.qdrant.service.GrpcMcpService;

import io.qdrant.client.grpc.Points;

import java.util.Map;

/**
 * Regression tests for the search contract: metadata filter constraints must
 * reach the Qdrant SearchPoints as a must-filter, and client-supplied limits
 * must be clamped before they reach Qdrant.
 */
public class SearchContractUnitTest {

    @Test
    void testMetadataFilterBuiltFromMap() {
        QdrantRepository repository = new QdrantRepository(null, null, null);
        Map<String, String> filters = Map.of("document_id", "doc-1", "sequence", "2");

        Points.Filter filter = repository.buildMetadataFilter(filters);
        assertEquals(2, filter.getMustList().size());
    }

    @Test
    void testMetadataFilterNullForEmptyConstraints() {
        QdrantRepository repository = new QdrantRepository(null, null, null);
        assertEquals(null, repository.buildMetadataFilter(Map.of()));
        assertEquals(null, repository.buildMetadataFilter(null));
    }

    @Test
    void testSearchLimitClamped() {
        assertEquals(100, GrpcMcpService.clampLimit(2000000000, 5, 100));
        assertEquals(5, GrpcMcpService.clampLimit(0, 5, 100));
        assertEquals(3, GrpcMcpService.clampLimit(3, 5, 100));
        assertEquals(1000, GrpcMcpService.clampLimit(2000000000, 100, 1000));
    }
}

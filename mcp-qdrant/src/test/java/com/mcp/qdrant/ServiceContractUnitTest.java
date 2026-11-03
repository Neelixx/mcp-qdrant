package com.mcp.qdrant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import com.mcp.qdrant.config.CacheConfig;
import com.mcp.qdrant.service.GrpcMcpService;

import io.qdrant.client.grpc.Collections;

/**
 * Unit tests for the service-layer contracts that are pure logic: distance
 * metric validation and per-collection cache naming. Everything that needs a
 * Qdrant client (search, listing, ingestion) is covered by the live end-to-end
 * tests instead.
 */
public class ServiceContractUnitTest {

    @Test
    void testResolveDistance_CaseInsensitive() {
        assertEquals(Collections.Distance.Cosine,
                GrpcMcpService.resolveDistance("cosine", Collections.Distance.Cosine));
        assertEquals(Collections.Distance.Euclid,
                GrpcMcpService.resolveDistance(" EUCLID ", Collections.Distance.Cosine));
        assertEquals(Collections.Distance.Dot,
                GrpcMcpService.resolveDistance("dOt", Collections.Distance.Cosine));
    }

    @Test
    void testResolveDistance_UnspecifiedKeepsDefault() {
        assertEquals(Collections.Distance.Cosine,
                GrpcMcpService.resolveDistance(null, Collections.Distance.Cosine));
        assertEquals(Collections.Distance.Cosine,
                GrpcMcpService.resolveDistance("   ", Collections.Distance.Cosine));
    }

    @Test
    void testResolveDistance_RejectsUnknownMetric() {
        try {
            GrpcMcpService.resolveDistance("hamming", Collections.Distance.Cosine);
            fail("An unsupported distance metric must not fall back to a default");
        } catch (IllegalArgumentException failure) {
            assertTrue(failure.getMessage().contains("Unsupported distance metric"));
            assertTrue(failure.getMessage().contains("hamming"));
        }
    }

    @Test
    void testDocumentListCacheName_PerCollection() {
        assertEquals(CacheConfig.DOCUMENT_LIST_CACHE_PREFIX + "-vpms",
                CacheConfig.getDocumentListCacheName("vpms"));
        assertEquals(CacheConfig.DOCUMENT_INFO_CACHE_PREFIX + "-vpms",
                CacheConfig.getDocumentInfoCacheName("vpms"));
    }

    @Test
    void testDocumentListCacheName_AcrossAllCollections() {
        assertEquals(CacheConfig.DOCUMENT_LIST_CACHE_PREFIX,
                CacheConfig.getDocumentListCacheName(null));
        assertEquals(CacheConfig.DOCUMENT_LIST_CACHE_PREFIX,
                CacheConfig.getDocumentListCacheName(""));
        assertEquals(CacheConfig.DOCUMENT_INFO_CACHE_PREFIX,
                CacheConfig.getDocumentInfoCacheName(""));
    }
}

package com.mcp.qdrant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.mcp.qdrant.proto.CollectionInfo;
import com.mcp.qdrant.proto.CreateCollectionRequest;
import com.mcp.qdrant.proto.CreateCollectionResponse;
import com.mcp.qdrant.proto.DeleteCollectionRequest;
import com.mcp.qdrant.proto.GetCollectionInfoRequest;
import com.mcp.qdrant.proto.GetCollectionInfoResponse;
import com.mcp.qdrant.proto.ListCollectionsRequest;
import com.mcp.qdrant.proto.ListCollectionsResponse;

/**
 * Unit tests for collection management operations.
 * Tests verify request/response handling without requiring a running Qdrant instance.
 */
public class CollectionManagementUnitTest {

    @Test
    void testCreateCollectionRequest_Builder() {
        String collectionName = "test-collection";
        int dimension = 512;
        String distance = "Cosine";

        CreateCollectionRequest request = CreateCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .setDimension(dimension)
                .setDistance(distance)
                .build();

        assertEquals(collectionName, request.getCollectionName());
        assertEquals(dimension, request.getDimension());
        assertEquals(distance, request.getDistance());
    }

    @Test
    void testCreateCollectionResponse_Builder() {
        String collectionName = "test-collection";
        String errorMessage = "Collection already exists";

        CreateCollectionResponse successResponse = CreateCollectionResponse.newBuilder()
                .setSuccess(true)
                .setCollectionName(collectionName)
                .build();

        CreateCollectionResponse errorResponse = CreateCollectionResponse.newBuilder()
                .setSuccess(false)
                .setCollectionName(collectionName)
                .setErrorMessage(errorMessage)
                .build();

        assertTrue(successResponse.getSuccess());
        assertFalse(errorResponse.getSuccess());
        assertEquals(errorMessage, errorResponse.getErrorMessage());
    }

    @Test
    void testDeleteCollectionRequest_Builder() {
        String collectionName = "collection-to-delete";

        DeleteCollectionRequest request = DeleteCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .build();

        assertEquals(collectionName, request.getCollectionName());
    }

    @Test
    void testListCollectionsRequest_Empty() {
        ListCollectionsRequest request = ListCollectionsRequest.newBuilder().build();
        assertNotNull(request);
    }

    @Test
    void testListCollectionsResponse_Builder() {
        CollectionInfo info1 = CollectionInfo.newBuilder()
                .setName("collection-1")
                .setPointsCount(100)
                .setVectorDimension(384)
                .setDistanceMetric("Cosine")
                .setStatus("green")
                .build();

        CollectionInfo info2 = CollectionInfo.newBuilder()
                .setName("collection-2")
                .setPointsCount(200)
                .setVectorDimension(512)
                .setDistanceMetric("Euclid")
                .setStatus("green")
                .build();

        ListCollectionsResponse response = ListCollectionsResponse.newBuilder()
                .setSuccess(true)
                .addCollections(info1)
                .addCollections(info2)
                .build();

        assertTrue(response.getSuccess());
        assertEquals(2, response.getCollectionsCount());
        assertEquals("collection-1", response.getCollections(0).getName());
        assertEquals("collection-2", response.getCollections(1).getName());
    }

    @Test
    void testGetCollectionInfoRequest_Builder() {
        String collectionName = "test-info-collection";

        GetCollectionInfoRequest request = GetCollectionInfoRequest.newBuilder()
                .setCollectionName(collectionName)
                .build();

        assertEquals(collectionName, request.getCollectionName());
    }

    @Test
    void testGetCollectionInfoResponse_Builder() {
        CollectionInfo info = CollectionInfo.newBuilder()
                .setName("test-collection")
                .setPointsCount(500)
                .setVectorDimension(768)
                .setDistanceMetric("Dot")
                .setStatus("green")
                .build();

        GetCollectionInfoResponse response = GetCollectionInfoResponse.newBuilder()
                .setSuccess(true)
                .setInfo(info)
                .build();

        assertTrue(response.getSuccess());
        assertTrue(response.hasInfo());
        assertEquals("test-collection", response.getInfo().getName());
        assertEquals(500, response.getInfo().getPointsCount());
    }

    @Test
    void testCollectionInfo_DefaultValues() {
        CollectionInfo info = CollectionInfo.newBuilder()
                .setName("default-collection")
                .build();

        assertEquals("default-collection", info.getName());
        assertEquals(0, info.getPointsCount());
        assertEquals(0, info.getVectorDimension());
        assertTrue(info.getDistanceMetric().isEmpty());
        assertTrue(info.getStatus().isEmpty());
    }
}

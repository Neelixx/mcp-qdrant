package com.mcp.qdrant;

import com.mcp.qdrant.proto.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for backup and restore operations.
 * Tests verify request/response message construction.
 */
public class BackupRestoreUnitTest {

    @Test
    void testBackupCollectionRequest_Builder() {
        String collectionName = "test-backup-collection";
        String backupPath = "/backups/custom";

        BackupCollectionRequest request = BackupCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .setBackupPath(backupPath)
                .build();

        assertEquals(collectionName, request.getCollectionName());
        assertEquals(backupPath, request.getBackupPath());
    }

    @Test
    void testBackupCollectionResponse_Builder() {
        String collectionName = "test-collection";
        String snapshotName = "test-collection-2024-01-15.snapshot";
        long sizeBytes = 10485760L; // 10MB
        String creationTime = "2024-01-15T10:30:00Z";
        String downloadUrl = "http://localhost:6333/collections/test-collection/snapshots/test-collection-2024-01-15.snapshot";

        BackupCollectionResponse response = BackupCollectionResponse.newBuilder()
                .setSuccess(true)
                .setCollectionName(collectionName)
                .setSnapshotName(snapshotName)
                .setSizeBytes(sizeBytes)
                .setCreationTime(creationTime)
                .setDownloadUrl(downloadUrl)
                .build();

        assertTrue(response.getSuccess());
        assertEquals(collectionName, response.getCollectionName());
        assertEquals(snapshotName, response.getSnapshotName());
        assertEquals(sizeBytes, response.getSizeBytes());
        assertEquals(creationTime, response.getCreationTime());
        assertEquals(downloadUrl, response.getDownloadUrl());
    }

    @Test
    void testBackupCollectionResponse_Error() {
        BackupCollectionResponse response = BackupCollectionResponse.newBuilder()
                .setSuccess(false)
                .setCollectionName("non-existent")
                .setErrorMessage("Collection not found")
                .build();

        assertFalse(response.getSuccess());
        assertEquals("Collection not found", response.getErrorMessage());
    }

    @Test
    void testRestoreCollectionRequest_Builder() {
        String collectionName = "restored-collection";
        String snapshotPath = "/backups/snapshot.snapshot";
        boolean overwrite = true;

        RestoreCollectionRequest request = RestoreCollectionRequest.newBuilder()
                .setCollectionName(collectionName)
                .setSnapshotPath(snapshotPath)
                .setOverwriteExisting(overwrite)
                .build();

        assertEquals(collectionName, request.getCollectionName());
        assertEquals(snapshotPath, request.getSnapshotPath());
        assertTrue(request.getOverwriteExisting());
    }

    @Test
    void testRestoreCollectionResponse_Builder() {
        String collectionName = "restored-collection";
        int pointsRestored = 1500;

        RestoreCollectionResponse response = RestoreCollectionResponse.newBuilder()
                .setSuccess(true)
                .setCollectionName(collectionName)
                .setPointsRestored(pointsRestored)
                .build();

        assertTrue(response.getSuccess());
        assertEquals(collectionName, response.getCollectionName());
        assertEquals(pointsRestored, response.getPointsRestored());
    }

    @Test
    void testRestoreCollectionResponse_Error() {
        RestoreCollectionResponse response = RestoreCollectionResponse.newBuilder()
                .setSuccess(false)
                .setCollectionName("test-collection")
                .setErrorMessage("Snapshot file not found")
                .build();

        assertFalse(response.getSuccess());
        assertEquals("Snapshot file not found", response.getErrorMessage());
    }

    @Test
    void testRestoreCollectionRequest_NoOverwrite() {
        RestoreCollectionRequest request = RestoreCollectionRequest.newBuilder()
                .setCollectionName("test-collection")
                .setSnapshotPath("/path/to/snapshot")
                .setOverwriteExisting(false)
                .build();

        assertFalse(request.getOverwriteExisting());
    }

    @Test
    void testBackupCollectionRequest_Minimal() {
        // Test minimal request with only required field
        BackupCollectionRequest request = BackupCollectionRequest.newBuilder()
                .setCollectionName("minimal-collection")
                .build();

        assertEquals("minimal-collection", request.getCollectionName());
        assertTrue(request.getBackupPath().isEmpty());
    }
}

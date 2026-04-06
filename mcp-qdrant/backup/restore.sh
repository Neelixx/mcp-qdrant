#!/bin/bash

# restore.sh - Restore Qdrant collection from snapshot file
# Usage: ./restore.sh <target_collection_name> <snapshot_file> [qdrant_host] [qdrant_port]
# Default host: localhost, default port: 6333

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

TARGET_COLLECTION="${1:-}"
SNAPSHOT_FILE="${2:-}"
QDRANT_HOST="${3:-localhost}"
QDRANT_PORT="${4:-6333}"
QDRANT_URL="http://${QDRANT_HOST}:${QDRANT_PORT}"

if [ -z "$TARGET_COLLECTION" ] || [ -z "$SNAPSHOT_FILE" ]; then
    echo "Usage: $0 <target_collection_name> <snapshot_file> [qdrant_host] [qdrant_port]"
    echo "Example: $0 qdrant-doc-restored ./backup/qdrant-doc-1234567890-2024-01-03-12-00-00.snapshot localhost 6333"
    echo ""
    echo "Available backups in ./backup/:"
    if [ -d "$SCRIPT_DIR" ]; then
        ls -lh "$SCRIPT_DIR"/*.snapshot 2>/dev/null || echo "  (no .snapshot files found)"
    fi
    exit 1
fi

# Check if snapshot file exists
if [ ! -f "$SNAPSHOT_FILE" ]; then
    echo "ERROR: Snapshot file not found: $SNAPSHOT_FILE"
    exit 1
fi

FILE_SIZE=$(du -h "$SNAPSHOT_FILE" | cut -f1)

echo "=== Qdrant Collection Restore ==="
echo "Target Collection: $TARGET_COLLECTION"
echo "Snapshot File: $SNAPSHOT_FILE"
echo "File Size: $FILE_SIZE"
echo "Qdrant URL: $QDRANT_URL"
echo ""

# Check if target collection already exists
echo "Checking if target collection exists..."
if curl -s -o /dev/null -w "%{http_code}" "${QDRANT_URL}/collections/${TARGET_COLLECTION}" | grep -q "200"; then
    echo "WARNING: Collection '$TARGET_COLLECTION' already exists!"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Restore cancelled."
        exit 0
    fi
    
    echo "Deleting existing collection..."
    curl -s -X DELETE "${QDRANT_URL}/collections/${TARGET_COLLECTION}" || true
    echo "Collection deleted."
fi

echo ""
echo "Uploading and restoring snapshot..."
echo "This may take a while for large collections..."

# Get just the filename for the upload
SNAPSHOT_FILENAME=$(basename "$SNAPSHOT_FILE")

# Upload and restore using Qdrant REST API
# POST /collections/{collection_name}/snapshots/upload?priority=snapshot
RESTORE_RESPONSE=$(curl -s -X POST \
    "${QDRANT_URL}/collections/${TARGET_COLLECTION}/snapshots/upload?priority=snapshot" \
    -H "Content-Type: multipart/form-data" \
    -F "snapshot=@${SNAPSHOT_FILE};filename=${SNAPSHOT_FILENAME}")

# Check response
if echo "$RESTORE_RESPONSE" | grep -q '"status":"ok"'; then
    echo ""
    echo "=== Restore Complete ==="
    echo "Collection: $TARGET_COLLECTION"
    echo "Restored from: $SNAPSHOT_FILENAME"
    echo ""
    
    # Get collection info
    sleep 1  # Wait a moment for collection to be ready
    INFO_RESPONSE=$(curl -s "${QDRANT_URL}/collections/${TARGET_COLLECTION}")
    POINTS_COUNT=$(echo "$INFO_RESPONSE" | grep -o '"points_count":[0-9]*' | cut -d':' -f2 || echo "unknown")
    
    echo "Collection info:"
    echo "  Points count: $POINTS_COUNT"
    echo ""
    echo "Verify with:"
    echo "  curl http://${QDRANT_HOST}:${QDRANT_PORT}/collections/${TARGET_COLLECTION}"
else
    echo ""
    echo "ERROR: Restore failed"
    echo "Response: $RESTORE_RESPONSE"
    exit 1
fi

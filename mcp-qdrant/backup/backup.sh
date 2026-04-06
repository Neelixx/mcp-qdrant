#!/bin/bash

# backup.sh - Create and download Qdrant collection snapshots
# Usage: ./backup.sh <collection_name> [qdrant_host] [qdrant_port]
# Default host: localhost, default port: 6333

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="$SCRIPT_DIR"

COLLECTION_NAME="${1:-}"
QDRANT_HOST="${2:-localhost}"
QDRANT_PORT="${3:-6333}"
QDRANT_URL="http://${QDRANT_HOST}:${QDRANT_PORT}"

if [ -z "$COLLECTION_NAME" ]; then
    echo "Usage: $0 <collection_name> [qdrant_host] [qdrant_port]"
    echo "Example: $0 qdrant-doc localhost 6333"
    exit 1
fi

echo "=== Qdrant Collection Backup ==="
echo "Collection: $COLLECTION_NAME"
echo "Qdrant URL: $QDRANT_URL"
echo "Backup directory: $BACKUP_DIR"
echo ""

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Check if collection exists
echo "Checking if collection exists..."
if ! curl -s -o /dev/null -w "%{http_code}" "${QDRANT_URL}/collections/${COLLECTION_NAME}" | grep -q "200"; then
    echo "ERROR: Collection '$COLLECTION_NAME' not found"
    exit 1
fi

echo "Collection exists. Creating snapshot..."

# Create snapshot using Qdrant REST API
SNAPSHOT_RESPONSE=$(curl -s -X POST \
    "${QDRANT_URL}/collections/${COLLECTION_NAME}/snapshots" \
    -H "Content-Type: application/json")

# Extract snapshot name from response
SNAPSHOT_NAME=$(echo "$SNAPSHOT_RESPONSE" | grep -o '"name":"[^"]*"' | cut -d'"' -f4)

if [ -z "$SNAPSHOT_NAME" ]; then
    echo "ERROR: Failed to create snapshot"
    echo "Response: $SNAPSHOT_RESPONSE"
    exit 1
fi

echo "Snapshot created: $SNAPSHOT_NAME"

# Download the snapshot
SNAPSHOT_URL="${QDRANT_URL}/collections/${COLLECTION_NAME}/snapshots/${SNAPSHOT_NAME}"
OUTPUT_FILE="${BACKUP_DIR}/${COLLECTION_NAME}_${SNAPSHOT_NAME}"

echo "Downloading snapshot..."
echo "URL: $SNAPSHOT_URL"
echo "Output: $OUTPUT_FILE"

if ! curl -s -L "$SNAPSHOT_URL" -o "$OUTPUT_FILE"; then
    echo "ERROR: Failed to download snapshot"
    exit 1
fi

# Get file size
FILE_SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)

echo ""
echo "=== Backup Complete ==="
echo "Collection: $COLLECTION_NAME"
echo "Snapshot: $SNAPSHOT_NAME"
echo "Saved to: $OUTPUT_FILE"
echo "Size: $FILE_SIZE"
echo ""
echo "To restore this collection:"
echo "  ./restore.sh ${COLLECTION_NAME}_restore $OUTPUT_FILE"

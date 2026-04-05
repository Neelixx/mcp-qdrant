#!/bin/bash
# Batch ingest all markdown files into vpmshelp collection

KNOWLEDGE_DIR="/home/frankw/CascadeProjects/windsurf-project/knowledges/vpmshelp"
GRPC_URL="localhost:9090"
BATCH_SIZE=10
TOTAL=0
SUCCESS=0
FAILED=0

# Create temp directory for batch files
TMPDIR=$(mktemp -d)
trap "rm -rf $TMPDIR" EXIT

log() {
    echo "[$(date '+%H:%M:%S')] $1"
}

# Function to ingest a single file
ingest_file() {
    local file="$1"
    local doc_id=$(echo "$file" | sed 's|/home/frankw/CascadeProjects/windsurf-project/knowledges/vpmshelp/||' | tr '/' '_')
    
    # Create JSON payload
    jq -Rs --arg doc_id "$doc_id" '{
        "document_id": $doc_id,
        "content": .,
        "target_collections": ["vpmshelp"],
        "metadata": {
            "source_path": $doc_id,
            "ingested_at": now | todate
        }
    }' "$file" > "$TMPDIR/payload.json" 2>/dev/null
    
    if [ $? -ne 0 ]; then
        log "SKIP: $doc_id (JSON encoding error)"
        return 1
    fi
    
    # Call gRPC
    grpcurl -plaintext -d @ "$GRPC_URL" com.vpms.mcp.qdrant.McpQdrantService/IngestDocument < "$TMPDIR/payload.json" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        return 0
    else
        return 1
    fi
}

# Main loop
log "Starting batch ingestion of .md files into vpmshelp..."
log "Directory: $KNOWLEDGE_DIR"
log "================================"

while IFS= read -r file; do
    TOTAL=$((TOTAL + 1))
    
    ingest_file "$file"
    if [ $? -eq 0 ]; then
        SUCCESS=$((SUCCESS + 1))
        log "OK ($SUCCESS/$TOTAL): $(basename "$file")"
    else
        FAILED=$((FAILED + 1))
        log "FAIL ($FAILED): $(basename "$file")"
    fi
    
    # Progress every 100 files
    if [ $((TOTAL % 100)) -eq 0 ]; then
        log "=== PROGRESS: $TOTAL files processed ($SUCCESS success, $FAILED failed) ==="
    fi
    
done < <(find "$KNOWLEDGE_DIR" -type f -name "*.md" | sort)

log "================================"
log "COMPLETE: $TOTAL files processed"
log "Success: $SUCCESS"
log "Failed: $FAILED"

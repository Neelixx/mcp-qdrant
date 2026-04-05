#!/bin/bash
# Test ingest first 100 markdown files into vpmshelp

KNOWLEDGE_DIR="/home/frankw/CascadeProjects/windsurf-project/knowledges/vpmshelp"
GRPC_URL="localhost:9090"
LIMIT=100
TOTAL=0
SUCCESS=0
FAILED=0

log() {
    echo "[$(date '+%H:%M:%S')] $1"
}

log "Testing ingestion of first $LIMIT files into vpmshelp..."
log "================================"

find "$KNOWLEDGE_DIR" -type f -name "*.md" | sort | head -$LIMIT | while read -r file; do
    TOTAL=$((TOTAL + 1))
    doc_id=$(echo "$file" | sed 's|/home/frankw/CascadeProjects/windsurf-project/knowledges/vpmshelp/||' | tr '/' '_')
    
    # Create JSON and ingest
    result=$(jq -Rs --arg doc_id "$doc_id" '{
        "document_id": $doc_id,
        "content": .,
        "target_collections": ["vpmshelp"]
    }' "$file" 2>/dev/null | grpcurl -plaintext -d @ "$GRPC_URL" com.vpms.mcp.qdrant.McpQdrantService/IngestDocument 2>/dev/null)
    
    if [ $? -eq 0 ] && echo "$result" | grep -q '"success": true'; then
        SUCCESS=$((SUCCESS + 1))
        chunks=$(echo "$result" | jq -r '.chunks_indexed // 0')
        log "OK ($SUCCESS): $doc_id ($chunks chunks)"
    else
        FAILED=$((FAILED + 1))
        log "FAIL ($FAILED): $doc_id"
    fi
    
    if [ $((TOTAL % 10)) -eq 0 ]; then
        log "--- Progress: $TOTAL/$LIMIT ---"
    fi
done

log "================================"
log "Test complete: $TOTAL files"
log "Success: $SUCCESS, Failed: $FAILED"

#!/bin/bash
# Batch ingest all markdown files into defined collection

INGEST_COLLECTION=vpms
KNOWLEDGE_DIR="./knowledges/$INGEST_COLLECTION"
GRPC_URL="localhost:9090"
BATCH_SIZE=10
CHUNK_SIZE=1000  # Characters per chunk (conservative for 2048 token limit)
OVERLAP=100
TOTAL=0
SUCCESS=0
FAILED=0

# Create temp directory for batch files
TMPDIR=$(mktemp -d)
trap "rm -rf $TMPDIR" EXIT

log() {
    echo "[$(date '+%H:%M:%S')] $1"
}

# Function to chunk a file into smaller pieces
chunk_file() {
    local file="$1"
    local base_id="$2"
    local chunks_file="$3"
    
    local chunk_num=0
    local content=""
    local chunk_content=""
    local in_frontmatter=0
    local frontmatter=""
    
    # Read file and chunk it
    while IFS= read -r line || [[ -n "$line" ]]; do
        if [[ "$line" == "---" ]]; then
            if [[ $in_frontmatter -eq 0 ]]; then
                in_frontmatter=1
                frontmatter="---"
                continue
            else
                in_frontmatter=0
                frontmatter="$frontmatter---"
                continue
            fi
        fi
        
        if [[ $in_frontmatter -eq 1 ]]; then
            frontmatter="$frontmatter$line"
            continue
        fi
        
        content="$content$line "
    done < "$file"
    
    local content_len=${#content}
    
    if [[ $content_len -le $CHUNK_SIZE ]]; then
        printf "%s\n\n%s" "$frontmatter" "$content" > "$chunks_file.0"
        echo 1
        return
    fi
    
    local pos=0
    local chunk_count=0
    
    while [[ $pos -lt $content_len ]]; do
        local chunk_end=$((pos + CHUNK_SIZE))
        if [[ $chunk_end -gt $content_len ]]; then
            chunk_end=$content_len
        fi
        
        chunk_content="${content:$pos:$((chunk_end - pos))}"
        printf "%s\n\n[CHUNK %d]\n%s" "$frontmatter" $((chunk_count + 1)) "$chunk_content" > "$chunks_file.$chunk_count"
        
        chunk_count=$((chunk_count + 1))
        pos=$((chunk_end - OVERLAP))
        
        if [[ $chunk_count -gt 200 ]]; then
            break
        fi
    done
    
    echo $chunk_count
}

# Function to ingest a single chunk - returns response
ingest_chunk() {
    local chunk_file="$1"
    local doc_id="$2"
    local chunk_num="$3"
    local total_chunks="$4"
    
    local chunk_id="${doc_id}_c${chunk_num}"
    
    jq -Rs --arg doc_id "$chunk_id" --arg parent_id "$doc_id" --arg chunk_num "$chunk_num" --arg total "$total_chunks" '{
        "document_id": $doc_id,
        "content": .,
        "target_collections": ["'"$INGEST_COLLECTION"'"],
        "metadata": {
            "source_path": $doc_id,
            "parent_document": $parent_id,
            "chunk_number": $chunk_num,
            "total_chunks": $total,
            "ingested_at": now | todate
        }
    }' "$chunk_file" > "$TMPDIR/payload.json" 2>/dev/null
    
    if [ $? -ne 0 ]; then
        echo '{"error":"JSON encoding failed"}'
        return 1
    fi
    
    local response
    response=$(grpcurl -plaintext -d @ "$GRPC_URL" com.vpms.mcp.qdrant.McpQdrantService/IngestDocument < "$TMPDIR/payload.json" 2>/dev/null)
    echo "$response"
    
    # Check if response contains error_message
    if echo "$response" | grep -q '"error_message"'; then
        return 1
    fi
    if echo "$response" | grep -q '"success": false'; then
        return 1
    fi
    return 0
}

# Function to ingest a single file (always chunked)
ingest_file() {
    local file="$1"
    local doc_id=$(echo "$file" | sed "s|$KNOWLEDGE_DIR||" | tr '/' '_')
    local basename=$(basename "$file")
    
    # Always chunk - even small files can be token-dense
    local chunks_file="$TMPDIR/chunks_$RANDOM"
    local chunk_count=$(chunk_file "$file" "$doc_id" "$chunks_file")
    
    if [[ $chunk_count -eq 0 ]]; then
        log "SKIP: $basename (empty file)"
        return 1
    fi
    
    local chunk_success=0
    local last_error=""
    
    for ((i=0; i<chunk_count; i++)); do
        local response
        response=$(ingest_chunk "${chunks_file}.$i" "$doc_id" "$((i+1))" "$chunk_count")
        local status=$?
        
        if [[ $status -eq 0 ]]; then
            chunk_success=$((chunk_success + 1))
        else
            # Extract error message
            last_error=$(echo "$response" | grep -o '"error_message":"[^"]*"' | head -1)
            if [[ -z "$last_error" ]]; then
                last_error="Unknown error"
            fi
        fi
        rm -f "${chunks_file}.$i"
    done
    
    if [[ $chunk_success -eq $chunk_count ]]; then
        log "OK: $basename ($chunk_count chunks)"
        return 0
    else
        log "FAIL: $basename ($chunk_success/$chunk_count chunks) - $last_error"
        return 1
    fi
}

# Main loop
log "Starting batch ingestion of .md files into $INGEST_COLLECTION..."
log "Directory: $KNOWLEDGE_DIR"
log "Chunk size: $CHUNK_SIZE chars (all files chunked for safety)"
log "================================"

while IFS= read -r file; do
    TOTAL=$((TOTAL + 1))
    
    ingest_file "$file"
    if [ $? -eq 0 ]; then
        SUCCESS=$((SUCCESS + 1))
    else
        FAILED=$((FAILED + 1))
    fi
    
    if [ $((TOTAL % 100)) -eq 0 ]; then
        log "=== PROGRESS: $TOTAL files processed ($SUCCESS success, $FAILED failed) ==="
    fi
    
done < <(find "$KNOWLEDGE_DIR" -type f -name "*.md" | sort)

log "================================"
log "COMPLETE: $TOTAL files processed"
log "Success: $SUCCESS"
log "Failed: $FAILED"

# Implementation Guide: MCP Server Integration with Qdrant

**Target File:** `mcp-qdrant-implementation.md`
**Status:** ✅ **IMPLEMENTED**

## Project Overview

A Spring Boot-based MCP (Model Context Protocol) server providing gRPC access to Qdrant vector database with hybrid search capabilities and document ingestion.

---

## 1. Core Search Workflow (`HybridSearch` RPC)

**Objective:** Perform unified, contextual search across configured Qdrant collections.

**Implementation:** `com.mcp.qdrant.service.GrpcMcpService`

### Workflow Steps:

1.  **Vectorization:**
    *   Calls `EmbeddingServiceClient.embed(query_text)` to obtain 768-dimensional query vector.
    *   Timeout: 50ms (configured via `mcp.embedding.timeout-ms`).
    *   Model: `nomic-embed-text-v2-moe` via Ollama.

2.  **Collection Iteration:**
    *   Retrieves configured collections: `vpms`, `vpmshelp`.
    *   Iterates through all collections, calling `QdrantRepository.search()` for each.
    *   **Fallback:** If one collection fails, logs error and continues to next collection.

3.  **Result Processing:**
    *   Aggregates results from all collections.
    *   Sorts by relevance score and applies `limit` (default: 5 if not specified).
    *   **Smart Summarization:** Only generates LLM summary if:
        - `summarize=true` is explicitly requested AND
        - `MCP_EMBEDDING_MODEL` is configured and not empty
    *   Sets `fallback_used=true` if summarization was requested but skipped (model unavailable).
    *   Builds document sources table showing `document_id`, `collection`, and `chunk_count`.
    *   Returns `HybridSearchResponse` with results, sources, and optional summary.

### gRPC Contract:
```protobuf
rpc HybridSearch(HybridSearchRequest) returns (HybridSearchResponse);
```

---

## 2. Document Ingestion (`IngestDocument` RPC)

**Objective:** Process and index documents into Qdrant collections.

**Implementation:** `com.mcp.qdrant.service.GrpcMcpService`

### Workflow Steps:

1.  **Chunking:**
    *   Uses `TextChunker` with configurable chunk size (default 512) and overlap (default 50).
    *   Preserves metadata for each chunk (sequence number, document ID).

2.  **Batch Vectorization:**
    *   Calls `EmbeddingServiceClient.embedBatch()` for all chunks.
    *   Batch size: 32 (configured via `mcp.embedding.batch-size`).

3.  **Indexing:**
    *   Calls `QdrantRepository.batchIndex()` to upsert vectors with metadata.
    *   Auto-creates collection if it doesn't exist.

### gRPC Contract:
```protobuf
rpc IngestDocument(IngestDocumentRequest) returns (IngestDocumentResponse);
```

---

## 3. Collection Management (`CreateCollection`, `DeleteCollection`, `ListCollections`, `GetCollectionInfo` RPCs)

**Objective:** Explicitly manage Qdrant collections with configurable vector parameters.

**Implementation:** `com.mcp.qdrant.service.GrpcMcpService`

### Workflow Steps:

1.  **CreateCollection:**
    *   Accepts collection name, dimension (default: 768), and distance metric (Cosine, Euclid, Dot).
    *   Creates collection using `QdrantClient.createCollectionAsync()` with proper `VectorParams`.
    *   Returns success/failure status.

2.  **DeleteCollection:**
    *   Accepts collection name.
    *   Deletes collection using `QdrantClient.deleteCollectionAsync()`.
    *   Returns success/failure status.

3.  **ListCollections:**
    *   Retrieves all collection names from Qdrant.
    *   Returns list of `CollectionInfo` objects with basic metadata.

4.  **GetCollectionInfo:**
    *   Accepts collection name.
    *   Retrieves detailed collection info: points count, status, vector dimension.
    *   Returns `CollectionInfo` with full metadata.

### gRPC Contracts:
```protobuf
rpc CreateCollection(CreateCollectionRequest) returns (CreateCollectionResponse);
rpc DeleteCollection(DeleteCollectionRequest) returns (DeleteCollectionResponse);
rpc ListCollections(ListCollectionsRequest) returns (ListCollectionsResponse);
rpc GetCollectionInfo(GetCollectionInfoRequest) returns (GetCollectionInfoResponse);
```

---

## 4. Component Architecture

### 4.1 EmbeddingServiceClient
**Location:** `com.mcp.qdrant.client.EmbeddingServiceClient`

- Handles HTTP calls to Ollama embedding service
- Methods: `embed()`, `embedBatch()`
- Error handling: Throws `RuntimeException` on timeout/failure

### 4.2 QdrantRepository
**Location:** `com.mcp.qdrant.repository.QdrantRepository`

- **search():** Performs vector similarity search
- **batchIndex():** Batch upsert of document chunks
- **ensureCollectionExists():** Auto-creates collections with proper vector params
- **getQdrantClient():** Exposes underlying QdrantClient for collection management operations

### 4.3 TextChunker
**Location:** `com.mcp.qdrant.chunker.TextChunker`

- Recursive text splitting by separators: `\n\n`, `\n`, `. `, ` `
- Configurable: `chunkSize`, `chunkOverlap`, `separators`

### 4.4 SummarizationService
**Location:** `com.mcp.qdrant.service.SummarizationService`

- Simple text-based summarization (concatenation + truncation)
- Extensible for LLM-based summarization

### 4.5 HealthController
**Location:** `com.mcp.qdrant.controller.HealthController`

- Provides root-level `/health` endpoint for Windsurf/Cascade MCP integration
- Returns JSON response with status, service name, version, and timestamp
- Independent controller (not under `/mcp` prefix) for MCP protocol compliance

### 4.6 McpHttpController
**Location:** `com.mcp.qdrant.controller.McpHttpController`

- HTTP REST API for MCP protocol compliance
- Routes MCP method calls to appropriate gRPC service methods
- Methods: `listCollections`, `getCollectionInfo`, `hybridSearch`, `ingestDocument`, `createCollection`, `deleteCollection`

---

## 5. Configuration

**File:** `application.yml`

```yaml
mcp:
  qdrant:
    host: localhost
    port: 6334
    collections: [vpms, vpmshelp]
    search-limit: 10
    search-threshold: 0.7
  embedding:
    service-url: http://localhost:11434
    model: nomic-embed-text-v2-moe
    dimension: 768
    timeout-ms: 50
    batch-size: 32
grpc:
  server:
    port: 9091
```

---

## 6. API Interface and Error Handling

- **gRPC Server:** Port 9091 (Docker: mapped to 9090)
- **Health Endpoint:** HTTP 8080 `/health`
- **Reflection:** Enabled for service discovery

### Error Handling:
- Connection timeouts logged with SLF4J
- Collection search failures don't block other collections
- Embedding service failures return gRPC error status

### Services Exposed:
- `com.mcp.qdrant.McpQdrantService` - Main MCP operations
- `grpc.health.v1.Health` - Health checks
- `grpc.reflection.v1alpha.ServerReflection` - Service discovery
- `McpHttpController` - HTTP REST API for MCP protocol
- `HealthController` - Root-level health endpoint

---

## 7. Deployment

### Docker Compose Stack:
- **Qdrant:** `qdrant/qdrant:latest` (ports 6333, 6334)
- **Ollama:** `ollama/ollama:rocm` (port 11434, GPU-enabled)
- **MCP Server:** Custom build (ports 8080, 9090)

### Build:
```bash
mvn clean package -DskipTests
docker-compose up --build
```

---

## 8. Lessons Learned

### 8.1 Lombok and Java 25 Compatibility
**Issue:** Lombok 1.18.34 failed to process annotations with Java 25 JDK.

**Solution:** Removed all Lombok dependencies and rewrote explicit code:
- Replaced `@Data` with explicit getters/setters
- Replaced `@Builder` with inner builder classes
- Replaced `@Slf4j` with explicit `LoggerFactory.getLogger()`
- Removed annotation processor from `maven-compiler-plugin`

**Impact:** ~300 lines of boilerplate added, but full Java 25 compatibility achieved.

---

### 8.2 Alpine Linux and gRPC Protoc
**Issue:** `protoc-gen-grpc-java` binary failed with "program not found or not executable" in Alpine-based Docker image.

**Root Cause:** Alpine uses musl libc; the protoc plugin requires glibc.

**Solution:** Changed Dockerfile builder stage from `eclipse-temurin:21-jdk-alpine` to `eclipse-temurin:21-jdk` (Debian-based with glibc).

---

### 8.3 Primitive float[] Streaming
**Issue:** `Arrays.stream(float[])` doesn't compile - primitive arrays don't work with generic stream API.

**Solution:** Created explicit helper method:
```java
private List<Float> toFloatList(float[] array) {
    List<Float> list = new ArrayList<>(array.length);
    for (float f : array) {
        list.add(f);
    }
    return list;
}
```

---

### 8.4 javax.annotation.Generated
**Issue:** Protobuf-generated code uses `javax.annotation.Generated` which was removed in Java 11+.

**Solution:** Added `javax.annotation-api` dependency (not `jakarta.annotation-api`):
```xml
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
</dependency>
```

---

### 8.5 Qdrant Client API Changes
**Issue:** `QdrantClient.createCollectionAsync()` signature changed between versions.

**Original (broken):**
```java
qdrantClient.createCollectionAsync(name, Distance.Cosine, dimension, null)
```

**Working:**
```java
Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
    .setDistance(Collections.Distance.Cosine)
    .setSize(dimension)
    .build();
qdrantClient.createCollectionAsync(name, vectorParams)
```

---

### 8.6 Port Configuration Strategy
**Issue:** Port 9090 already in use on development machine.

**Solution:** 
- Internal gRPC port: `9091` (configured in `application.yml`)
- Docker external mapping: `9090:9091`
- Local development: direct `9091` access

This allows flexibility for different deployment scenarios.

---

## 9. Technology Stack

- **Java:** 21 (LTS) - built with Java 25 JDK
- **Framework:** Spring Boot 3.2.0
- **gRPC:** grpc-server-spring-boot-starter 2.15.0
- **Qdrant Client:** 1.8.1
- **Build:** Maven 3.9+
- **Virtual Threads:** Enabled

---

*Last Updated: April 2026*
# MCP Qdrant Server

[![Status](https://img.shields.io/badge/status-implemented-success)](./)
[![Java](https://img.shields.io/badge/Java-21%2B-blue)](./)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)](./)

A production-ready Java Spring Boot gRPC service for hybrid vector search and document ingestion using Qdrant vector database and external embedding services.

**Key Features:**

- ✅ **Hybrid Search** across multiple Qdrant collections with result aggregation
- ✅ **Document Ingestion** with intelligent text chunking and batch indexing  
- ✅ **Collection Management** - Create, delete, list, and get info for collections
- ✅ **Document Caching** with Spring Cache + Caffeine - per-collection dynamic sizing
- ✅ **Backup & Restore** - Snapshot-based collection backup and restore
- ✅ **HTTP API** for MCP protocol with `/health` endpoint
- ✅ **GPU Acceleration** via ROCm/Ollama for AMD GPUs
- ✅ **Java 25 Compatible** - Lombok-free implementation
- ✅ **Comprehensive Test Suite** - Unit and integration tests with Testcontainers

## System Requirements

| Component      | Requirement                                          |
| -------------- | ---------------------------------------------------- |
| Java           | 21 (LTS) - tested with Java 25                       |
| Maven          | 3.9+                                                 |
| Docker         | 24.0+ (for containerized deployment)                 |
| Docker Compose | 2.20+                                                |
| GPU (Optional) | AMD GPU with ROCm support (tested on RX 9070 XT)     |
| Memory         | 8GB+ RAM (64GB recommended for large-scale indexing) |

### Tested Environment

- **OS**: CachyOS Linux (Kernel 6.19.11)
- **Java**: OpenJDK 25.0.2 (Temurin)
- **CPU**: AMD Ryzen 7 9800X3D (16 threads)
- **GPU**: AMD Radeon RX 9070 XT
- **Memory**: 64 GB RAM

## Architecture

```
┌─────────────────┐     gRPC      ┌──────────────────┐
│   MCP Client    │◄─────────────►│  GrpcMcpService  │
└─────────────────┘               └────────┬─────────┘
                                           │
           ┌───────────────────────────────┼───────────────┐
           │                               │               │
    ┌──────▼──────┐              ┌─────────▼────────┐      │
    │TextChunker  │              │ EmbeddingService │      │
    └─────────────┘              │      Client      │      │
                                 └─────────┬────────┘      │
                                          │                │
                                 ┌────────▼────────┐       │
                                 │  Ollama/ROCm    │       │
                                 │ (nomic-embed-   │       │
                                 │  text-v2-moe)   │       │
                                 └─────────────────┘       │
                                                           │
                                 ┌─────────────────────────▼──────┐
                                 │        QdrantRepository        │
                                 │  (Collections: vpms, vpmshelp) │
                                 └────────────────────────────────┘
```

## Quick Start

### Option 1: Docker Compose (Recommended)

1. **Start the full stack:**
   
   ```bash
   cd mcp-qdrant
   docker-compose up --build -d
   ```

2. **Verify all services:**
   
   ```bash
   # Check containers
   docker-compose ps
   
   # Qdrant REST API
   curl http://localhost:6333
   
   # Ollama (list models)
   curl http://localhost:11434/api/tags
   
   # MCP gRPC health
   grpcurl -plaintext localhost:9090 list
   
   # MCP health endpoint
   curl http://localhost:8080/health
   ```

### Option 2: Local Development

1. **Start dependencies:**
   
   ```bash
   # Qdrant
   docker run -d --name qdrant -p 6333:6333 -p 6334:6334 qdrant/qdrant:latest
   
   # Ollama (ROCm for AMD GPUs)
   docker run -d --name ollama --gpus all -p 11434:11434 \
     -v ollama_data:/root/.ollama \
     ollama/ollama:rocm
   ```

2. **Pull embedding model:**
   
   ```bash
   curl -X POST http://localhost:11434/api/pull \
     -d '{"name": "nomic-embed-text-v2-moe"}'
   ```

3. **Build and run:**
   
   ```bash
   mvn clean package -DskipTests
   java -jar target/mcp-qdrant-0.0.2.jar
   ```

4. **Verify:**
   
   ```bash
   # Health check
   curl http://localhost:8080/health
   
   # gRPC services (port 9091 for local)
   grpcurl -plaintext localhost:9091 list
   ```

## GitHub Copilot & Windsurf Integration

To use this server with GitHub Copilot or Windsurf, add the following configuration:

### VS Code

**File:** `~/.vscode/mcp-settings.json` or workspace settings

```json
{
  "servers": {
    "com.mcp/mcp-qdrant": {
      "type": "stdio",
      "command": "mcp-qdrant/mcp-bridge.sh"
    }
  }
}
```

### Windsurf

**File:** `~/.windsurf/mcp_config.json`

```json
{
  "servers": {
    "com.mcp/mcp-qdrant": {
      "type": "stdio",
      "command": "mcp-qdrant/mcp-bridge.sh"
    }
  }
}
```

### Available Tools

| Tool                   | Description                                             |
| ---------------------- | ------------------------------------------------------- |
| `listCollections`      | List all Qdrant collections with cache size & doc count |
| `getCollectionInfo`    | Get collection metadata and point count                 |
| `createCollection`     | Create a new collection with vector params              |
| `deleteCollection`     | Delete an existing collection                           |
| `hybridSearch`         | Search across collections with vector + keyword         |
| `ingestDocument`       | Chunk and index documents into collections              |
| `listDocuments`        | List all documents in a collection                      |
| `deleteDocument`       | Delete a document by ID                                 |
| `getDocumentInfo`      | Get document metadata and chunk count                   |
| `rebuildDocumentCache` | Rebuild cache and recalculate sizes per collection      |
| `backupCollection`     | Create a snapshot backup of a collection                |
| `restoreCollection`    | Restore a collection from a snapshot                    |

### Zed

**File:** `~/.config/zed/settings.json`

```json
{
  "mcp": {
    "servers": {
      "mcp-qdrant": {
        "url": "http://localhost:8080/mcp",
        "headers": {}
      }
    }
  }
}
```

### MCP HTTP API

MCP Qdrant Server supports the **MCP protocol over HTTP** in addition to gRPC. This enables integration with editors like Zed that use HTTP-based MCP connections.

**Endpoint:** `POST http://localhost:8080/mcp`

The HTTP endpoint accepts standard MCP JSON-RPC requests:

```bash
curl -s http://localhost:8080/mcp \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "initialize",
    "id": 0
  }'
```

**Supported Methods:**
- `initialize` - MCP handshake returning server capabilities
- `notifications/initialized` - Client initialization confirmation
- `tools/list` - List all available tools
- `hybridSearch` - Search across collections
- `listCollections`, `getCollectionInfo`, `createCollection`, `deleteCollection`
- `backupCollection`, `restoreCollection`
- `ingestDocument`, `listDocuments`, `deleteDocument`, `getDocumentInfo`, `rebuildDocumentCache`

### Troubleshooting Connection Issues**

- Verify Docker containers are running: `docker ps`
- Check backend health: `curl http://localhost:8080/health`
- Review bridge logs: `ls /tmp/mcp-qdrant-logs/`

## Configuration

Configuration via `application.yml` or environment variables:

```yaml
mcp:
  qdrant:
    host: ${MCP_QDRANT_HOST:localhost}
    port: ${MCP_QDRANT_PORT:6334}
    api-key: ${QDRANT_API_KEY:}
    use-tls: false
    collections: ${MCP_QDRANT_COLLECTIONS:all}  # 'all' or comma-separated list
    search-limit: 10
    search-threshold: 0.7

  embedding:
    service-url: ${MCP_EMBEDDING_SERVICE_URL:http://localhost:11434}
    model: nomic-embed-text-v2-moe
    dimension: 768
    timeout-ms: 50
    batch-size: 32

grpc:
  server:
    port: 9091  # Docker: mapped to 9090

server:
  port: 8080
```

### Environment Variables

| Variable                    | Default                   | Description                                                                              |
| --------------------------- | ------------------------- | ---------------------------------------------------------------------------------------- |
| `MCP_QDRANT_HOST`           | `localhost`               | Qdrant server hostname                                                                   |
| `MCP_QDRANT_PORT`           | `6334`                    | Qdrant gRPC port                                                                         |
| `MCP_QDRANT_COLLECTIONS`    | `all`                     | Collection mode: `all` (dynamic), `coll1,coll2` (specific), or `""` (treated as `all`) |
| `MCP_QDRANT_SUMMARYMAXRESULTS` | `3`                    | Max results to include in summary                                                        |
| `MCP_QDRANT_SUMMARYMAXLENGTH`  | `500`                  | Max characters per result in summary                                                   |
| `MCP_SUMMARIZE_MODEL`       | (none)                    | LLM model for summarization (e.g., `llama3.2`). If not set, uses simple concatenation  |
| `QDRANT_API_KEY`            | (none)                    | API key for Qdrant Cloud                                                                 |
| `MCP_EMBEDDING_SERVICE_URL` | `http://localhost:11434`  | Ollama/embedding service URL                                                             |
| `MCP_EMBEDDING_MODEL`       | `nomic-embed-text-v2-moe` | Embedding model name                                                                     |
| `MCP_EMBEDDING_TIMEOUT_MS`  | `10000`                   | Embedding service timeout                                                                |
| `GRPC_SERVER_PORT`          | `9091`                    | gRPC server port                                                                         |
| **HTTP API**                | 8080                      | MCP protocol endpoint, health checks                                                     |
| **gRPC**                    | 9091                      | Internal gRPC service (Docker: 9090)                                                     |

## gRPC API

### Services

| Service                                    | Description         |
| ------------------------------------------ | ------------------- |
| `com.mcp.qdrant.McpQdrantService`          | Main MCP operations |
| `grpc.health.v1.Health`                    | Health checks       |
| `grpc.reflection.v1alpha.ServerReflection` | Service discovery   |

### Methods

**HybridSearch**

```protobuf
rpc HybridSearch(HybridSearchRequest) returns (HybridSearchResponse);
```

- Searches across all configured collections
- **Parameters**:
  - `query_text` (required): Search query
  - `limit` (optional): Number of results to return (default: 5)
  - `summarize` (optional): Request LLM summary (default: false)
  - `filters` (optional): Metadata filters as key-value pairs
- **Returns**:
  - Aggregated search results sorted by relevance score
  - Document sources table (document_id, collection, chunk_count)
  - Generated summary (LLM-generated if `MCP_SUMMARIZE_MODEL` configured, otherwise simple concatenation)
  - `fallback_used` flag indicating if summarization was skipped or LLM failed

**Smart Summarization Behavior**:

| Condition | Summary Type | `fallback_used` |
|-----------|--------------|-----------------|
| `summarize=true` + `MCP_SUMMARIZE_MODEL` configured → LLM summary via Ollama `/api/generate` | false |
| `summarize=true` but LLM fails/unavailable → Simple concatenation fallback | true |
| `summarize=false` or not specified → No summary (default) | false |

**Simple Concatenation Fallback**: When no LLM is configured or LLM fails, returns top N results (configurable via `MCP_QDRANT_SUMMARYMAXRESULTS`, default 3) with truncated content (`MCP_QDRANT_SUMMARYMAXLENGTH`, default 500 chars).

**Collection Configuration Behavior**:

- `MCP_QDRANT_COLLECTIONS=all` (or not set): Dynamically fetches all collections from Qdrant for each search/ingestion
- `MCP_QDRANT_COLLECTIONS=coll1,coll2`: Uses only those specific collections
- `MCP_QDRANT_COLLECTIONS=""` (empty): Treated as `all`

This allows flexible deployment scenarios where collections may be created dynamically.

**IngestDocument**

```protobuf
rpc IngestDocument(IngestDocumentRequest) returns (IngestDocumentResponse);
```

- Chunks document and generates embeddings
- Indexes into specified collection

**Collection Management**

```protobuf
rpc CreateCollection(CreateCollectionRequest) returns (CreateCollectionResponse);
rpc DeleteCollection(DeleteCollectionRequest) returns (DeleteCollectionResponse);
rpc BackupCollection(BackupCollectionRequest) returns (BackupCollectionResponse);
rpc RestoreCollection(RestoreCollectionRequest) returns (RestoreCollectionResponse);
```

- Create/delete collections with configurable vector dimensions
- **Backup**: Creates a snapshot of collection data with download URL
- **Restore**: Uploads and restores collection from snapshot file
- List all collections with metadata including cache size and document count
- Get detailed info for specific collection

**Document Caching**

```protobuf
rpc ListDocuments(ListDocumentsRequest) returns (ListDocumentsResponse);
rpc DeleteDocument(DeleteDocumentRequest) returns (DeleteDocumentResponse);
rpc GetDocumentInfo(GetDocumentInfoRequest) returns (GetDocumentInfoResponse);
rpc RebuildDocumentCache(RebuildDocumentCacheRequest) returns (RebuildDocumentCacheResponse);
```

- **Per-collection cache sizing**: Cache size is dynamically calculated as `max(1000, totalDocuments)`
  - Small collections (250 docs): 1000 cache entries
  - Large collections (5000+ docs): Cache size grows with document count
- **Cache lifecycle**:
  - `createCollection`: Initializes cache with default size (1000)
  - `deleteCollection`: Removes cache for the collection
  - `restoreCollection`: Automatically rebuilds cache based on restored data
  - `ingestDocument`/`deleteDocument`: Invalidates cache entries
- **rebuildDocumentCache**: Manually trigger cache recalculation after bulk operations

### Backup & Restore Scripts

For command-line backup/restore operations, use the provided shell scripts in the `backup/` directory:

**Backup a collection:**

```bash
cd mcp-qdrant/backup
./backup.sh <collection_name> [qdrant_host] [qdrant_port]
```

Example:

```bash
./backup.sh qdrant-doc localhost 6333
```

This creates a compressed snapshot file in the `backup/` directory.

**Restore a collection:**

```bash
./restore.sh <target_collection_name> <snapshot_file> [qdrant_host] [qdrant_port]
```

Example:

```bash
./restore.sh qdrant-doc-restored ./qdrant-doc-*.snapshot localhost 6333
```

The restore script will prompt before overwriting existing collections.

### Proto Definition

See `src/main/proto/mcp_contracts.proto` for full message schemas.

## Project Structure

```
mcp-qdrant/
├── pom.xml                          # Maven configuration
├── docker-compose.yml               # Full stack orchestration
├── Dockerfile                       # Multi-stage build
├── backup/                          # Backup/restore scripts
│   ├── backup.sh                    # Create and download snapshots
│   └── restore.sh                   # Upload and restore snapshots
├── src/main/
│   ├── proto/mcp_contracts.proto    # gRPC service definitions
│   ├── resources/application.yml    # Spring configuration
│   └── java/com/mcp/qdrant/
│       ├── McpQdrantApplication.java
│       ├── controller/              # HTTP controllers
│       │   ├── HealthController.java
│       │   └── McpHttpController.java
│       ├── config/                  # Configuration classes
│       ├── service/                 # gRPC service + summarization
│       ├── repository/              # Qdrant data access
│       ├── client/                  # Embedding HTTP client
│       ├── chunker/                 # Text chunking logic
│       └── model/                   # Domain models
```

## Development

### Building

```bash
# Compile (generates protobuf sources)
mvn clean compile

# Run tests
mvn test

# Package executable JAR
mvn clean package -DskipTests

# The JAR will be at: target/mcp-qdrant-0.0.2.jar
```

### Key Implementation Notes

- **No Lombok**: All boilerplate (getters/setters/builders) written explicitly for Java 25 compatibility
- **Virtual Threads**: Enabled via `spring.threads.virtual.enabled: true`
- **Float Arrays**: Custom `toFloatList()` helper for Qdrant vector handling (primitive float[] not directly streamable)
- **Docker Builder**: Uses `eclipse-temurin:21-jdk` (non-Alpine) for glibc compatibility with protoc-gen-grpc-java

### IDE Setup (Eclipse/IntelliJ)

1. **Import as Maven project**
2. **Update Maven**: `Alt+F5` → Check "Force Update" (Eclipse)
3. **Refresh after compile**: Generated sources in `target/generated-sources/protobuf`

## Testing

The project includes unit tests that validate proto contracts and message builders.

### Test Structure

```
src/test/java/com/mcp/qdrant/
├── CollectionManagementUnitTest.java   # 8 tests
├── DocumentOperationsUnitTest.java     # 8 tests
├── BackupRestoreUnitTest.java          # 8 tests
└── ProtoContractUnitTest.java          # 8 tests
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CollectionManagementUnitTest
```

### Unit Tests (32 tests)

| Test Class                     | Tests | Coverage                                      |
| ------------------------------ | ----- | --------------------------------------------- |
| `CollectionManagementUnitTest` | 8     | Create, delete, list, getInfo proto contracts |
| `DocumentOperationsUnitTest`   | 8     | Ingest, search, chunking message builders     |
| `BackupRestoreUnitTest`        | 8     | Backup/restore requests/responses             |
| `ProtoContractUnitTest`        | 8     | All proto message type validation             |

## Troubleshooting

### Maven Build Errors

**"testcontainers version is empty"**

- Solution: Force Maven update (`Alt+F5` in Eclipse)

**"Protobuf classes not found"**

- Solution: Run `mvn compile` to generate sources

### Runtime Issues

**Qdrant connection refused**

- Check Qdrant is running: `docker ps | grep qdrant`
- Verify `MCP_QDRANT_HOST` and `MCP_QDRANT_PORT`

**Embedding service timeout**

- Check Ollama is running: `curl http://localhost:11434/api/tags`
- Verify model is pulled: `ollama list`
- Increase `MCP_EMBEDDING_TIMEOUT_MS` (default: 50ms)

### GPU/ROCm Issues

**Ollama not using GPU**

- Verify ROCm drivers: `rocm-smi`
- Check device permissions: `/dev/kfd` and `/dev/dri` accessible
- View Ollama logs: `docker logs ollama`

## References

- [Qdrant Documentation](https://qdrant.tech/documentation/)
- [Ollama API](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [gRPC Java](https://grpc.io/docs/languages/java/)
- [MCP Specification](https://spec.modelcontextprotocol.io/)

## License

Apache License 2.0 - See LICENSE file for details.

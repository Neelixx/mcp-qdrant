# MCP Qdrant Server

[![Status](https://img.shields.io/badge/status-implemented-success)](./)
[![Java](https://img.shields.io/badge/Java-21%2B-blue)](./)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)](./)

A production-ready Java Spring Boot gRPC service for hybrid vector search and document ingestion using Qdrant vector database and external embedding services.

**Key Features:**
- ✅ **Hybrid Search** across multiple Qdrant collections with result aggregation
- ✅ **Document Ingestion** with intelligent text chunking and batch indexing  
- ✅ **Collection Management** - Create, delete, list, and get info for collections
- ✅ **HTTP API** for MCP protocol with `/health` endpoint
- ✅ **GPU Acceleration** via ROCm/Ollama for AMD GPUs
- ✅ **Java 25 Compatible** - Lombok-free implementation

## System Requirements

| Component | Requirement |
|-----------|-------------|
| Java | 21 (LTS) - tested with Java 25 |
| Maven | 3.9+ |
| Docker | 24.0+ (for containerized deployment) |
| Docker Compose | 2.20+ |
| GPU (Optional) | AMD GPU with ROCm support (tested on RX 9070 XT) |
| Memory | 8GB+ RAM (64GB recommended for large-scale indexing) |

### Tested Environment

- **OS**: CachyOS Linux (Kernel 6.19.11)
- **Java**: OpenJDK 25.0.2 (Temurin)
- **CPU**: AMD Ryzen 7 9800X3D (16 threads)
- **GPU**: AMD Radeon RX 9070 XT
- **Memory**: 64 GB RAM

## Architecture

```
┌─────────────────┐     gRPC      ┌──────────────────┐
│   MCP Client    │◄──────────────►│  GrpcMcpService  │
└─────────────────┘               └────────┬─────────┘
                                           │
           ┌───────────────────────────────┼───────────────┐
           │                               │               │
    ┌──────▼──────┐              ┌─────────▼────────┐     │
    │TextChunker  │              │EmbeddingService │     │
    └─────────────┘              │     Client      │     │
                                 └─────────┬────────┘     │
                                          │                │
                                 ┌────────▼────────┐      │
                                 │  Ollama/ROCm    │      │
                                 │ (nomic-embed-   │      │
                                 │  text-v2-moe)   │      │
                                 └─────────────────┘      │
                                                          │
                                 ┌────────────────────────▼─────────┐
                                 │        QdrantRepository        │
                                 │  (Collections: vpms, vpmshelp) │
                                 └────────────────────────────────┘
```

## Quick Start

### Option 1: Docker Compose (Recommended)

1. **Start the full stack:**
   ```bash
   cd /home/frankw/CascadeProjects/windsurf-project/mcp-qdrant
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
      "command": "/home/frankw/git/github/mcp-qdrant/mcp-qdrant/mcp-bridge.sh"
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
      "command": "/home/frankw/git/github/mcp-qdrant/mcp-qdrant/mcp-bridge.sh"
    }
  }
}
```

### Available Tools

| Tool | Description |
|------|-------------|
| `listCollections` | List all Qdrant collections |
| `getCollectionInfo` | Get collection metadata and point count |
| `hybridSearch` | Search across collections with vector + keyword |
| `ingestDocument` | Chunk and index documents into collections |
| `backupCollection` | Create a snapshot backup of a collection |
| `restoreCollection` | Restore a collection from a snapshot |

### Troubleshooting Connection Issues

**"Connection closed" error:**
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

| Variable | Default | Description |
|----------|---------|-------------|
| `MCP_QDRANT_HOST` | `localhost` | Qdrant server hostname |
| `MCP_QDRANT_PORT` | `6334` | Qdrant gRPC port |
| `MCP_QDRANT_COLLECTIONS` | `all` | Collection mode: `all` (dynamic), `coll1,coll2` (specific), or `\"\"` (treated as `all`) |
| `QDRANT_API_KEY` | (none) | API key for Qdrant Cloud |
| `MCP_EMBEDDING_SERVICE_URL` | `http://localhost:11434` | Ollama/embedding service URL |
| `MCP_EMBEDDING_MODEL` | `nomic-embed-text-v2-moe` | Embedding model name |
| `MCP_EMBEDDING_TIMEOUT_MS` | `10000` | Embedding service timeout |
| `GRPC_SERVER_PORT` | `9091` | gRPC server port |
| **HTTP API** | 8080 | MCP protocol endpoint, health checks |
| **gRPC** | 9091 | Internal gRPC service (Docker: 9090) |

## gRPC API

### Services

| Service | Description |
|---------|-------------|
| `com.mcp.qdrant.McpQdrantService` | Main MCP operations |
| `grpc.health.v1.Health` | Health checks |
| `grpc.reflection.v1alpha.ServerReflection` | Service discovery |

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
  - LLM-generated summary (if requested and model available)
  - `fallback_used` flag indicating if summarization was skipped

**Smart Summarization Behavior**:
- If `summarize=true` and `MCP_EMBEDDING_MODEL` is configured → LLM summary generated
- If `summarize=true` but model unavailable → `fallback_used=true`, no summary
- If `summarize=false` or not specified → Returns results without summary (default)

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
- List all collections with metadata
- Get detailed info for specific collection

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

MIT License - See LICENSE file for details.

# MCP Qdrant Server

[![Status](https://img.shields.io/badge/status-implemented-success)](./)
[![Java](https://img.shields.io/badge/Java-21%2B-blue)](./)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)](./)

A production-ready Java Spring Boot gRPC service for hybrid vector search and document ingestion using Qdrant vector database and external embedding services.

**Key Features:**
- ✅ **Hybrid Search** across multiple Qdrant collections with result aggregation
- ✅ **Document Ingestion** with intelligent text chunking and batch indexing  
- ✅ **Collection Management** - Create, delete, list, and get info for collections
- ✅ **gRPC API** with reflection and health checking
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
   curl http://localhost:8080/actuator/health
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
   curl http://localhost:8080/actuator/health

   # gRPC services (port 9091 for local)
   grpcurl -plaintext localhost:9091 list
   ```

## Configuration

Configuration via `application.yml` or environment variables:

```yaml
mcp:
  qdrant:
    host: ${MCP_QDRANT_HOST:localhost}
    port: ${MCP_QDRANT_PORT:6334}
    api-key: ${QDRANT_API_KEY:}
    use-tls: false
    collections:
      - vpms
      - vpmshelp
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
| `MCP_QDRANT_COLLECTIONS` | `vpms,vpmshelp` | Comma-separated default collections |
| `QDRANT_API_KEY` | (none) | API key for Qdrant Cloud |
| `MCP_EMBEDDING_SERVICE_URL` | `http://localhost:11434` | Ollama/embedding service URL |
| `MCP_EMBEDDING_MODEL` | `nomic-embed-text-v2-moe` | Embedding model name |
| `MCP_EMBEDDING_TIMEOUT_MS` | `10000` | Embedding service timeout |
| `GRPC_SERVER_PORT` | `9091` | gRPC server port |
| `SERVER_PORT` | `8080` | HTTP actuator port |

## gRPC API

### Services

| Service | Description |
|---------|-------------|
| `com.vpms.mcp.qdrant.McpQdrantService` | Main MCP operations |
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
rpc ListCollections(ListCollectionsRequest) returns (ListCollectionsResponse);
rpc GetCollectionInfo(GetCollectionInfoRequest) returns (GetCollectionInfoResponse);
```
- Create/delete collections with configurable vector dimensions
- List all collections with metadata
- Get detailed info for specific collection

### Proto Definition

See `src/main/proto/mcp_contracts.proto` for full message schemas.

## Project Structure

```
mcp-qdrant/
├── pom.xml                          # Maven configuration
├── docker-compose.yml               # Full stack orchestration
├── Dockerfile                       # Multi-stage build
├── src/main/
│   ├── proto/mcp_contracts.proto    # gRPC service definitions
│   ├── resources/application.yml    # Spring configuration
│   └── java/com/vpms/mcp/qdrant/
│       ├── McpQdrantApplication.java
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

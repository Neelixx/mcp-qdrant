# eveloping an MCP Server for Qdrant Access in Java

**Version: 0.0.2**

## System Environment

- Local Ollama in Docker on: http://localhost:11434
- Model optimized for: nomic-embed-text-v2-moe
- Embedding dimension: 768
- Collections: vpms, vpmshelp
- Target Environments: Eclipse plugin, also as docker container using docker-compose
- **Configuration:**
    - timeout: 50 (ms)
    - embeddingmodel: nomic-embed-text-v2-moe
- Project Location: Copilot-Memories/Memory-VPMS/mcp-qdrant
- Programming Language: Java 21

## Architecture & Design Principles (Architect's Note)

The design utilizes a multi-layered approach optimized for high concurrency and maintainability:
1.  **Communication Protocol:** **gRPC** is mandated for all internal, service-to-service communication (MCP Server $\leftrightarrow$ Embedding Service $\leftrightarrow$ Clients) due to its strong typing and binary performance benefits over REST/HTTP.
2.  **Separation of Concerns:**
    *   **SearchService (API Layer):** Handles external gRPC contracts; acts as an **Orchestrator**.
    *   **QdrantRepository (Data Access Layer):** Handles all interactions with Qdrant (Search, Ingest, and **Collection Management**).
    *   **EmbeddingServiceClient (External Dependency):** Handles calls to the external Nomic embedding service.
3.  **Concurrency:** Leverage Java 21's **Virtual Threads** within the Spring Boot framework to handle massive concurrent I/O operations (waiting for Qdrant or the Embedding Service) without thread overhead.

## Functional Requirements ## Functional Requirements & Workflows Workflows (Updated Specifications)

### 1. Core Search Workflow
The `HybridSearch` RPC call orchestrates the following sequence:
1.  Receive `query_text`.
2.  Call `EmbeddingServiceClient` to convert `query_text` into a 768-dimensional vector.
3.  Call `QdrantRepository` to execute a hybrid search using the vector and provided search parameters.
4.  Map raw Qdrant results into the `SearchResult` domain object and return them via gRPC.

### 2. Ingestion Workflow (Replacing Python Logic)
The `IngestDocument` RPC call orchestrates:
1.  Receive raw document content.
2.  Use a dedicated **`TextChunker`** component to split the document into semantically coherent chunks, preserving metadata for each chunk.
3.  Batch-call `EmbeddingServiceClient` for all chunks to receive vectors.
4.  Call `QdrantRepository.batchIndex` to insert the vectors, metadata, and document ID into the target collections (`vpms`, `vpmshelp`).



## References & Dependencies
- **External Components:** Qdrant Database, External Nomic Embedding Service.
- **Source Code Context:** `vector_db/search`, `vector_db/ingest`, `vector_db/cli`.
- **Contracts:** Defined by `mcp_contracts.proto` (gRPC Schema).
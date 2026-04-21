# MCP Qdrant Server - Tool Reference

This document provides comprehensive documentation for all MCP Qdrant Server tools, designed for external AI systems to programmatically interact with the server.

## Server Connection

- **HTTP Endpoint**: `http://localhost:8080/mcp`
- **Protocol**: JSON-RPC 2.0
- **Content-Type**: `application/json`

### Request Format
```json
{
  "jsonrpc": "2.0",
  "method": "<tool_name>",
  "params": { <parameters> },
  "id": 1
}
```

### Response Format
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": { <response_data> }
}
```

---

## Tool Reference

### 1. hybridSearch

Perform hybrid vector search across Qdrant collections with result aggregation.

**Parameters:**
- `queryText` (string, required): The search query text
- `limit` (integer, optional): Maximum number of results to return (default: 5)
- `summarize` (boolean, optional): Whether to summarize results using LLM (default: false)

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "hybridSearch",
  "params": {
    "queryText": "vector database architecture",
    "limit": 10,
    "summarize": true
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "results": [
      {
        "collection": "qdrant-doc",
        "score": 0.857,
        "payload": "...",
        "metadata": {
          "url": "/documentation/overview/",
          "text": "Qdrant is a vector database..."
        }
      }
    ],
    "summary": "Qdrant is a high-performance vector database...",
    "collectionsSearched": 3,
    "fallbackUsed": false
  }
}
```

---

### 2. listCollections

List all Qdrant collections with metadata including point counts, dimensions, and embedding models.

**Parameters:** None

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "listCollections",
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "collections": [
      {
        "name": "vpms",
        "pointsCount": 67223,
        "vectorDimension": 768,
        "distanceMetric": "Cosine",
        "status": "Green",
        "cacheSize": 1048576,
        "totalDocuments": 31172,
        "embeddingModel": "nomic-embed-text-v2-moe"
      }
    ]
  }
}
```

---

### 3. getCollectionInfo

Get detailed information about a specific Qdrant collection.

**Parameters:**
- `collectionName` (string, required): Name of the collection

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "getCollectionInfo",
  "params": {
    "collectionName": "vpms"
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "collection": {
      "name": "vpms",
      "pointsCount": 67223,
      "vectorDimension": 768,
      "distanceMetric": "Cosine",
      "status": "Green"
    }
  }
}
```

---

### 4. createCollection

Create a new Qdrant collection with specified vector configuration.

**Parameters:**
- `collectionName` (string, required): Name of the new collection
- `dimension` (integer, required): Vector dimension (e.g., 768, 384)
- `distance` (string, required): Distance metric ("Cosine", "Euclidean", "Dot", "Manhattan")

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "createCollection",
  "params": {
    "collectionName": "my_collection",
    "dimension": 768,
    "distance": "Cosine"
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "message": "Collection created successfully"
  }
}
```

---

### 5. deleteCollection

Delete a Qdrant collection and all its data.

**Parameters:**
- `collectionName` (string, required): Name of the collection to delete

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "deleteCollection",
  "params": {
    "collectionName": "my_collection"
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "message": "Collection deleted successfully"
  }
}
```

---

### 6. backupCollection

Create a snapshot backup of a Qdrant collection.

**Parameters:**
- `collectionName` (string, required): Name of the collection to backup
- `backupPath` (string, optional): Path for the backup file (default: collection-specific)

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "backupCollection",
  "params": {
    "collectionName": "vpms",
    "backupPath": "./backups/vpms-snapshot"
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "message": "Backup created at ./backups/vpms-snapshot"
  }
}
```

---

### 7. restoreCollection

Restore a Qdrant collection from a snapshot backup.

**Parameters:**
- `collectionName` (string, required): Name for the restored collection
- `snapshotPath` (string, required): Path to the snapshot file
- `overwriteExisting` (boolean, optional): Whether to overwrite if collection exists (default: false)

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "restoreCollection",
  "params": {
    "collectionName": "vpms-restored",
    "snapshotPath": "./backups/vpms-snapshot",
    "overwriteExisting": false
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "message": "Collection restored successfully"
  }
}
```

---

### 8. listDocuments

List documents in a collection with pagination support.

**Parameters:**
- `collectionName` (string, optional): Name of the collection (default: searches all)
- `limit` (integer, optional): Maximum documents to return (default: 100)
- `offset` (integer, optional): Number of documents to skip (default: 0)

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "listDocuments",
  "params": {
    "collectionName": "vpms",
    "limit": 10,
    "offset": 0
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "totalDocuments": 31172,
    "documents": [
      {
        "collection": "vpms",
        "documentId": "doc_001",
        "chunkCount": 12
      }
    ]
  }
}
```

---

### 9. getDocumentInfo

Get detailed information about a specific document.

**Parameters:**
- `documentId` (string, required): Unique identifier of the document
- `collectionName` (string, optional): Collection name (searches all if not specified)

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "getDocumentInfo",
  "params": {
    "documentId": "doc_001",
    "collectionName": "vpms"
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "document": {
      "documentId": "doc_001",
      "collection": "vpms",
      "chunkCount": 12,
      "metadata": {
        "title": "Document Title",
        "url": "https://example.com/doc"
      }
    }
  }
}
```

---

### 10. ingestDocument

Ingest a document into Qdrant with automatic text chunking and embedding.

**Parameters:**
- `documentId` (string, required): Unique identifier for the document
- `content` (string, required): Document content to ingest
- `targetCollections` (array of strings, optional): Collections to add document to (default: configured default collections)
- `chunkSize` (integer, optional): Size of text chunks in characters (default: 1000)
- `chunkOverlap` (integer, optional): Overlap between chunks in characters (default: 200)

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "ingestDocument",
  "params": {
    "documentId": "doc_001",
    "content": "This is the document content...",
    "targetCollections": ["vpms", "vpmshelp"],
    "chunkSize": 1000,
    "chunkOverlap": 200
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "message": "Document ingested successfully",
    "chunksCreated": 12,
    "collections": ["vpms", "vpmshelp"]
  }
}
```

---

### 11. deleteDocument

Delete a document from one or all collections.

**Parameters:**
- `documentId` (string, required): Unique identifier of the document to delete
- `collectionName` (string, optional): Specific collection to delete from (deletes from all if not specified)

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "deleteDocument",
  "params": {
    "documentId": "doc_001",
    "collectionName": "vpms"
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "message": "Document deleted successfully",
    "collectionsDeleted": ["vpms"]
  }
}
```

---

### 12. rebuildDocumentCache

Rebuild the document cache for improved performance on a specific collection.

**Parameters:**
- `collectionName` (string, optional): Name of the collection (rebuilds all if not specified)

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "rebuildDocumentCache",
  "params": {
    "collectionName": "vpms"
  },
  "id": 1
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": true,
    "message": "Document cache rebuilt successfully",
    "collection": "vpms"
  }
}
```

---

## Error Handling

All responses include a `success` boolean field. On failure:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "success": false,
    "errorMessage": "Error description here"
  }
}
```

Common error scenarios:
- Collection not found
- Invalid parameters
- Connection issues with Qdrant
- Embedding service unavailable

---

## Configuration Notes

### Default Collections
The server is configured with default collections (e.g., `vpms`, `vpmshelp`). If no collection is specified in parameters, operations may apply to all configured collections.

### Embedding Models
Different collections may use different embedding models:
- Default: `nomic-embed-text-v2-moe` (768 dimensions)
- Collection-specific: `all-minilm:l6-v2` (384 dimensions) for `qdrant-doc`

The server automatically handles embedding generation based on collection configuration.

### Summarization
When `summarize: true` is set in `hybridSearch`, results are summarized using the configured LLM (e.g., `gemma4:e2b` via Ollama). This requires the summarization model to be configured in the server.

---

## Usage Best Practices

1. **Search**: Always use `hybridSearch` for querying; it automatically searches across relevant collections
2. **Pagination**: Use `limit` and `offset` in `listDocuments` for large document sets
3. **Document IDs**: Use unique, stable document IDs for reliable updates/deletes
4. **Chunking**: Adjust `chunkSize` and `chunkOverlap` based on document type for optimal retrieval
5. **Caching**: Use `rebuildDocumentCache` after bulk ingestion for improved performance
6. **Backups**: Use `backupCollection` before destructive operations like `deleteCollection`

---

## Testing Connection

To verify server connectivity:

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"listCollections","id":1}'
```

Expected response: JSON with `success: true` and collections array.

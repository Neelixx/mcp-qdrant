#!/usr/bin/env python3
"""
MCP-compliant stdio server for mcp-qdrant - Copilot compatible.
"""

import sys
import json
import urllib.request
import urllib.error
import os

# Force unbuffered output
sys.stdout.reconfigure(line_buffering=True)

BACKEND_URL = os.environ.get("MCP_QDRANT_URL", "http://localhost:8080/mcp")

# ---------------------------------------------------------------------------
# Tool definitions (MCP schema format)
# ---------------------------------------------------------------------------
TOOLS = [
    {
        "name": "listCollections",
        "description": "List all Qdrant vector collections available on the MCP Qdrant server.",
        "inputSchema": {
            "type": "object",
            "properties": {},
            "required": []
        }
    },
    {
        "name": "getCollectionInfo",
        "description": "Get detailed information about a specific Qdrant collection.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "collectionName": {
                    "type": "string",
                    "description": "Name of the collection to query."
                }
            },
            "required": ["collectionName"]
        }
    },
    {
        "name": "hybridSearch",
        "description": "Perform a hybrid (vector + keyword) search across Qdrant collections.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "queryText": {
                    "type": "string",
                    "description": "The natural language query to search for."
                },
                "limit": {
                    "type": "integer",
                    "description": "Maximum number of results to return (default: 5).",
                    "default": 5
                },
                "summarize": {
                    "type": "boolean",
                    "description": "Whether to generate an LLM summary of the results (default: false).",
                    "default": False
                },
                "filters": {
                    "type": "object",
                    "description": "Optional key-value filters to apply to the search.",
                    "additionalProperties": {"type": "string"}
                }
            },
            "required": ["queryText"]
        }
    },
    {
        "name": "ingestDocument",
        "description": "Chunk and ingest a document into one or more Qdrant collections.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "documentId": {
                    "type": "string",
                    "description": "Unique identifier for the document."
                },
                "content": {
                    "type": "string",
                    "description": "The full text content of the document."
                },
                "targetCollections": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "List of collection names to ingest into."
                },
                "metadata": {
                    "type": "object",
                    "description": "Optional metadata key-value pairs.",
                    "additionalProperties": {"type": "string"}
                },
                "chunkSize": {
                    "type": "integer",
                    "description": "Chunk size in characters (default: 512).",
                    "default": 512
                },
                "chunkOverlap": {
                    "type": "integer",
                    "description": "Overlap between chunks in characters (default: 50).",
                    "default": 50
                }
            },
            "required": ["documentId", "content", "targetCollections"]
        }
    },
    {
        "name": "createCollection",
        "description": "Create a new Qdrant vector collection.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "collectionName": {
                    "type": "string",
                    "description": "Name for the new collection."
                },
                "dimension": {
                    "type": "integer",
                    "description": "Vector dimension (default: 768).",
                    "default": 768
                },
                "distance": {
                    "type": "string",
                    "description": "Distance metric: Cosine, Euclid, or Dot (default: Cosine).",
                    "enum": ["Cosine", "Euclid", "Dot"],
                    "default": "Cosine"
                }
            },
            "required": ["collectionName"]
        }
    },
    {
        "name": "deleteCollection",
        "description": "Delete an existing Qdrant collection and all its data.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "collectionName": {
                    "type": "string",
                    "description": "Name of the collection to delete."
                }
            },
            "required": ["collectionName"]
        }
    }
]

# ---------------------------------------------------------------------------
# Backend call
# ---------------------------------------------------------------------------
def call_backend(method, params):
    payload = json.dumps({
        "jsonrpc": "2.0",
        "id": 1,
        "method": method,
        "params": params
    }).encode("utf-8")
    req = urllib.request.Request(
        BACKEND_URL,
        data=payload,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.URLError as e:
        return {"jsonrpc": "2.0", "error": {"code": -32603, "message": str(e)}}


# ---------------------------------------------------------------------------
# Tool argument mapping: MCP camelCase -> backend method + params
# ---------------------------------------------------------------------------
def dispatch_tool(tool_name, arguments):
    if tool_name == "listCollections":
        result = call_backend("listCollections", {})
        return result.get("result", result.get("error", {}))

    elif tool_name == "getCollectionInfo":
        result = call_backend("getCollectionInfo",
                              {"collectionName": arguments["collectionName"]})
        return result.get("result", result.get("error", {}))

    elif tool_name == "hybridSearch":
        params = {
            "queryText": arguments["queryText"],
            "limit": arguments.get("limit", 5),
            "summarize": arguments.get("summarize", False),
            "filters": arguments.get("filters", {})
        }
        result = call_backend("hybridSearch", params)
        return result.get("result", result.get("error", {}))

    elif tool_name == "ingestDocument":
        params = {
            "documentId": arguments["documentId"],
            "content": arguments["content"],
            "targetCollections": arguments["targetCollections"],
            "metadata": arguments.get("metadata", {}),
            "chunkingConfig": {
                "chunkSize": arguments.get("chunkSize", 512),
                "chunkOverlap": arguments.get("chunkOverlap", 50),
                "separator": "\n"
            }
        }
        result = call_backend("ingestDocument", params)
        return result.get("result", result.get("error", {}))

    elif tool_name == "createCollection":
        params = {
            "collectionName": arguments["collectionName"],
            "dimension": arguments.get("dimension", 768),
            "distance": arguments.get("distance", "Cosine")
        }
        result = call_backend("createCollection", params)
        return result.get("result", result.get("error", {}))

    elif tool_name == "deleteCollection":
        result = call_backend("deleteCollection",
                              {"collectionName": arguments["collectionName"]})
        return result.get("result", result.get("error", {}))

    else:
        return {"error": f"Unknown tool: {tool_name}"}


# ---------------------------------------------------------------------------
# MCP message handler
# ---------------------------------------------------------------------------
def handle(msg):
    method = msg.get("method", "")
    msg_id = msg.get("id")           # None for notifications
    params = msg.get("params", {})

    # --- notifications/initialized -----------------------------------------
    if method == "notifications/initialized":
        return None

    # --- initialized (legacy) ----------------------------------------------
    if method == "initialized":
        return None

    # --- initialize ----------------------------------------------------------
    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {
                    "tools": {"listChanged": False}
                },
                "serverInfo": {
                    "name": "com.vpms/mcp-qdrant",
                    "version": "0.0.2"
                }
            }
        }

    # --- tools/list ----------------------------------------------------------
    if method == "tools/list":
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": {"tools": TOOLS}
        }

    # --- tools/call ----------------------------------------------------------
    if method == "tools/call":
        tool_name = params.get("name", "")
        arguments = params.get("arguments", {})
        try:
            tool_result = dispatch_tool(tool_name, arguments)
            return {
                "jsonrpc": "2.0",
                "id": msg_id,
                "result": {
                    "content": [
                        {
                            "type": "text",
                            "text": json.dumps(tool_result, indent=2, ensure_ascii=False)
                        }
                    ],
                    "isError": False
                }
            }
        except Exception as e:
            return {
                "jsonrpc": "2.0",
                "id": msg_id,
                "result": {
                    "content": [{"type": "text", "text": str(e)}],
                    "isError": True
                }
            }

    # --- ping ----------------------------------------------------------------
    if method == "ping":
        return {"jsonrpc": "2.0", "id": msg_id, "result": {}}

    # --- unknown method ------------------------------------------------------
    if msg_id is not None:
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "error": {"code": -32601, "message": f"Method not found: {method}"}
        }
    return None


# ---------------------------------------------------------------------------
# Main loop
# ---------------------------------------------------------------------------
def main():
    try:
        for line in sys.stdin:
            line = line.strip()
            if not line:
                continue
            try:
                msg = json.loads(line)
            except json.JSONDecodeError as e:
                resp = {"jsonrpc": "2.0", "id": None,
                        "error": {"code": -32700, "message": f"Parse error: {e}"}}
                print(json.dumps(resp), flush=True)
                continue

            response = handle(msg)
            if response is not None:
                print(json.dumps(response, ensure_ascii=False), flush=True)
    except Exception as e:
        # Log error to stderr and exit cleanly
        print(json.dumps({
            "jsonrpc": "2.0",
            "id": None,
            "error": {"code": -32603, "message": f"Internal error: {str(e)}"}
        }), flush=True, file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()

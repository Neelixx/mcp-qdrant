#!/usr/bin/env bash
# MCP Qdrant Server Startup Script with Environment Variables
# This script sets environment variables and starts the MCP Qdrant server

echo "========================================"
echo "MCP Qdrant Server with Environment Variables"
echo "========================================"
echo

cd "$(dirname "$0")/mcp-qdrant"

# Set environment variables (you can modify these as needed)
# Qdrant Configuration
export MCP_QDRANT_HOST=localhost
export MCP_QDRANT_PORT=6334
export MCP_QDRANT_COLLECTIONS=all
export MCP_QDRANT_TIMEOUT_MS=30000
export QDRANT_API_KEY=

# Embedding Service Configuration
export MCP_QDRANT_SUMMARYMAXRESULTS=10
export MCP_QDRANT_SUMMARYMAXLENGTH=1000
export MCP_QDRANT_SUMMARIZEMODEL=gemma4:latest
export MCP_EMBEDDING_SERVICE_URL=http://localhost:11434
export MCP_EMBEDDING_MODEL=all-minilm:l6-v2
export MCP_EMBEDDING_TIMEOUT_MS=10000

# Server Configuration
export GRPC_SERVER_PORT=9091

# Display environment variables being used
echo "Environment Variables:"
echo "  MCP_QDRANT_HOST:             $MCP_QDRANT_HOST"
echo "  MCP_QDRANT_PORT:             $MCP_QDRANT_PORT"
echo "  MCP_QDRANT_TIMEOUT_MS:       $MCP_QDRANT_TIMEOUT_MS"
echo "  MCP_QDRANT_COLLECTIONS:      $MCP_QDRANT_COLLECTIONS"
echo "  QDRANT_API_KEY:              $QDRANT_API_KEY"
echo "  MCP_QDRANT_SUMMARYMAXRESULTS:$MCP_QDRANT_SUMMARYMAXRESULTS"
echo "  MCP_QDRANT_SUMMARYMAXLENGTH: $MCP_QDRANT_SUMMARYMAXLENGTH"
echo "  MCP_QDRANT_SUMMARIZEMODEL:   $MCP_QDRANT_SUMMARIZEMODEL"
echo
echo "  MCP_EMBEDDING_SERVICE_URL:   $MCP_EMBEDDING_SERVICE_URL"
echo "  MCP_EMBEDDING_MODEL:         $MCP_EMBEDDING_MODEL"
echo "  MCP_EMBEDDING_TIMEOUT_MS:    $MCP_EMBEDDING_TIMEOUT_MS"
echo "  GRPC_SERVER_PORT:            $GRPC_SERVER_PORT"
echo

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 25 or higher and try again"
    exit 1
fi

# set maven on PATH:
export PATH=../apache-maven/bin:$PATH

echo "Current directory: $(pwd)"
echo

# Build the project
echo "Building MCP Qdrant server..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: Build failed"
    exit 1
fi
echo "build was successful."

# Check if JAR file exists
if [ ! -f "target/mcp-qdrant-0.0.2.jar" ]; then
    echo "ERROR: JAR file not found after build"
    echo "Expected location: target/mcp-qdrant-0.0.2.jar"
    exit 1
fi
echo
echo "Build completed successfully, server jar file exists!"
echo

# Start the server
echo "Starting MCP Qdrant server..."
echo "Server will be available at:"
echo "  - HTTP API: http://localhost:8080"
echo "  - Health Check: http://localhost:8080/health"
echo "  - gRPC: localhost:$GRPC_SERVER_PORT"
echo
echo "Press Ctrl+C to stop the server"
echo

java -jar target/mcp-qdrant-0.0.2.jar

echo
echo "Server stopped."
cd ..

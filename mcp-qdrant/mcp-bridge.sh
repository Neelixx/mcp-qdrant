#!/bin/bash
# Wrapper script for mcp-bridge.py with logging

LOG_DIR="/tmp/mcp-qdrant-logs"
mkdir -p "$LOG_DIR"

# Timestamp for this run
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
LOG_FILE="$LOG_DIR/mcp-bridge-$TIMESTAMP.log"

# Log startup
echo "=== MCP Bridge Started at $(date) ===" >> "$LOG_FILE"
echo "Command: $0" >> "$LOG_FILE"
echo "Arguments: $@" >> "$LOG_FILE"
echo "Working dir: $(pwd)" >> "$LOG_FILE"
echo "Python version: $(python3 --version)" >> "$LOG_FILE"
echo "" >> "$LOG_FILE"

# Run the bridge with input/output logging
exec /usr/bin/python3 -u /home/frankw/git/github/mcp-qdrant/mcp-qdrant/mcp-bridge.py 2>> "$LOG_FILE"

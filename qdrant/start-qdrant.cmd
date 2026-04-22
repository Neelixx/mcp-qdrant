@echo off
cd /D "%~dp0"
set MCP_QDRANT_TIMEOUT_MS=30000
start "Qdrant Database" .\bin\qdrant.exe --config-path .\bin\qdrant-config.yaml
timeout /t 3
exit

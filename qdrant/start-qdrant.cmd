@echo off
cd /D "%~dp0"
start "Qdrant Database" .\bin\qdrant.exe --config-path .\bin\qdrant-config.yaml
timeout /t 3
exit

@echo off
REM MCP Qdrant Server Startup Script with Environment Variables
REM This script sets environment variables and starts the MCP Qdrant server

echo ========================================
echo MCP Qdrant Server with Environment Variables
echo ========================================
echo.
cd /d "%~dp0mcp-qdrant"

REM Set environment variables (you can modify these as needed)
REM Qdrant Configuration
set MCP_QDRANT_HOST=localhost
set MCP_QDRANT_PORT=6334
set MCP_QDRANT_COLLECTIONS=all
set QDRANT_API_KEY=

REM Embedding Service Configuration
set MCP_QDRANT_SUMMARYMAXRESULTS=10
set MCP_QDRANT_SUMMARYMAXLENGTH=1000
set MCP_QDRANT_SUMMARIZEMODEL=gemma4:e2b
set MCP_EMBEDDING_SERVICE_URL=http://localhost:11434
set MCP_EMBEDDING_MODEL=nomic-embed-text-v2-moe
set MCP_EMBEDDING_TIMEOUT_MS=10000

REM Server Configuration
set GRPC_SERVER_PORT=9091

REM Display environment variables being used
echo Environment Variables:
echo   MCP_QDRANT_HOST:             %MCP_QDRANT_HOST%
echo   MCP_QDRANT_PORT:             %MCP_QDRANT_PORT%
echo   MCP_QDRANT_COLLECTIONS:      %MCP_QDRANT_COLLECTIONS%
echo   QDRANT_API_KEY:              %QDRANT_API_KEY%
echo   MCP_QDRANT_SUMMARYMAXRESULTS:%MCP_QDRANT_SUMMARYMAXRESULTS%
echo   MCP_QDRANT_SUMMARYMAXLENGTH: %MCP_QDRANT_SUMMARYMAXLENGTH%
echo   MCP_QDRANT_SUMMARIZEMODEL:   %MCP_QDRANT_SUMMARIZEMODEL%


echo   MCP_EMBEDDING_SERVICE_URL:   %MCP_EMBEDDING_SERVICE_URL%
echo   MCP_EMBEDDING_MODEL:         %MCP_EMBEDDING_MODEL%
echo   MCP_EMBEDDING_TIMEOUT_MS:    %MCP_EMBEDDING_TIMEOUT_MS%
echo   GRPC_SERVER_PORT:            %GRPC_SERVER_PORT%
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 21 or higher and try again
    pause
    exit /b 1
)

REM set maven on PATH:
set PATH=..\apache-maven\bin;%PATH%

echo Current directory: %CD%
echo.

REM Build the project
echo Building MCP Qdrant server...
cmd /C mvn clean package -DskipTests
rem if not "%ERRORLEVEL%" == "0" (
rem     echo ERROR: Build failed
rem     pause
rem     exit /b 1
rem )
echo build was successful.

REM Check if JAR file exists
if not exist "target\mcp-qdrant-0.0.2.jar" (
    echo ERROR: JAR file not found after build
    echo Expected location: target\mcp-qdrant-0.0.2.jar
    pause
    exit /b 1
)
echo.
echo Build completed successfully, server jar file exists!
echo.

REM Start the server
echo Starting MCP Qdrant server...
echo Server will be available at:
echo   - HTTP API: http://localhost:8080
echo   - Health Check: http://localhost:8080/health
echo   - gRPC: localhost:%GRPC_SERVER_PORT%
echo.
echo Press Ctrl+C to stop the server
echo.

cmd /C java -jar target\mcp-qdrant-0.0.2.jar

echo.
echo Server stopped.
cd ..
pause


@echo off
REM backup.cmd - Create and download Qdrant collection snapshots (Windows)
REM Usage: backup.cmd <collection_name> [qdrant_host] [qdrant_port]
REM Default host: localhost, default port: 6333

setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "BACKUP_DIR=%SCRIPT_DIR%"

set "COLLECTION_NAME=%~1"
set "QDRANT_HOST=%~2"
set "QDRANT_PORT=%~3"

if "%~1"=="" (
    echo Usage: %~nx0 ^<collection_name^> [qdrant_host] [qdrant_port]
    echo Example: %~nx0 qdrant-doc localhost 6333
    exit /b 1
)

if "%QDRANT_HOST%"=="" set "QDRANT_HOST=localhost"
if "%QDRANT_PORT%"=="" set "QDRANT_PORT=6333"

set "QDRANT_URL=http://%QDRANT_HOST%:%QDRANT_PORT%"

echo === Qdrant Collection Backup ===
echo Collection: %COLLECTION_NAME%
echo Qdrant URL: %QDRANT_URL%
echo Backup directory: %BACKUP_DIR%
echo.

REM Check if collection exists using PowerShell
echo Checking if collection exists...
powershell -Command "try { $response = Invoke-RestMethod -Uri '%QDRANT_URL%/collections/%COLLECTION_NAME%' -Method GET -TimeoutSec 10; exit 0 } catch { exit 1 }"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Collection '%COLLECTION_NAME%' not found
    exit /b 1
)

echo Collection exists. Creating snapshot...

REM Create snapshot using PowerShell
for /f "delims=" %%a in ('powershell -Command "try { $response = Invoke-RestMethod -Uri '%QDRANT_URL%/collections/%COLLECTION_NAME%/snapshots' -Method POST -ContentType 'application/json' -TimeoutSec 60; Write-Output ($response.result.name) } catch { exit 1 }"') do set "SNAPSHOT_NAME=%%a"

if "%SNAPSHOT_NAME%"=="" (
    echo ERROR: Failed to create snapshot
    exit /b 1
)

echo Snapshot created: %SNAPSHOT_NAME%

REM Download the snapshot
set "SNAPSHOT_URL=%QDRANT_URL%/collections/%COLLECTION_NAME%/snapshots/%SNAPSHOT_NAME%"
set "OUTPUT_FILE=%BACKUP_DIR%%COLLECTION_NAME%_%SNAPSHOT_NAME%"

echo Downloading snapshot...
echo URL: %SNAPSHOT_URL%
echo Output: %OUTPUT_FILE%

powershell -Command "try { Invoke-RestMethod -Uri '%SNAPSHOT_URL%' -Method GET -TimeoutSec 300 -OutFile '%OUTPUT_FILE%'; exit 0 } catch { Write-Error $_.Exception.Message; exit 1 }"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to download snapshot
    exit /b 1
)

REM Get file size using PowerShell
for /f "delims=" %%a in ('powershell -Command "$size = (Get-Item '%OUTPUT_FILE%').Length; if($size -gt 1GB) { '{0:N2} GB' -f ($size/1GB) } elseif($size -gt 1MB) { '{0:N2} MB' -f ($size/1MB) } elseif($size -gt 1KB) { '{0:N2} KB' -f ($size/1KB) } else { '{0} B' -f $size }"') do set "FILE_SIZE=%%a"

echo.
echo === Backup Complete ===
echo Collection: %COLLECTION_NAME%
echo Snapshot: %SNAPSHOT_NAME%
echo Saved to: %OUTPUT_FILE%
echo Size: %FILE_SIZE%
echo.
echo To restore this collection:
echo   restore.cmd %COLLECTION_NAME%_restore "%OUTPUT_FILE%"

endlocal

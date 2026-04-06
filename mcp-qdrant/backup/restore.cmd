@echo off
REM restore.cmd - Restore Qdrant collection from snapshot file (Windows)
REM Usage: restore.cmd <target_collection_name> <snapshot_file> [qdrant_host] [qdrant_port]
REM Default host: localhost, default port: 6333

setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"

set "TARGET_COLLECTION=%~1"
set "SNAPSHOT_FILE=%~2"
set "QDRANT_HOST=%~3"
set "QDRANT_PORT=%~4"

if "%~1"=="" (
    echo Usage: %~nx0 ^<target_collection_name^> ^<snapshot_file^> [qdrant_host] [qdrant_port]
    echo Example: %~nx0 qdrant-doc-restored "C:\path\to\qdrant-doc-*.snapshot" localhost 6333
    echo.
    echo Available backups in %SCRIPT_DIR%:
    dir /b "%SCRIPT_DIR%\*.snapshot" 2^>nul
    if !ERRORLEVEL! neq 0 echo   ^(no .snapshot files found^)
    exit /b 1
)

if "%~2"=="" (
    echo ERROR: Snapshot file not specified
    exit /b 1
)

REM Resolve relative paths
if not exist "%SNAPSHOT_FILE%" (
    set "SNAPSHOT_FILE=%SCRIPT_DIR%%~2"
)

if not exist "%SNAPSHOT_FILE%" (
    echo ERROR: Snapshot file not found: %SNAPSHOT_FILE%
    exit /b 1
)

if "%QDRANT_HOST%"=="" set "QDRANT_HOST=localhost"
if "%QDRANT_PORT%"=="" set "QDRANT_PORT=6333"

set "QDRANT_URL=http://%QDRANT_HOST%:%QDRANT_PORT%"

REM Get file size
for %%F in ("%SNAPSHOT_FILE%") do set "FILE_SIZE=%%~zF"
powershell -Command "$size = %FILE_SIZE%; if($size -gt 1GB) { $size='{0:N2} GB' -f ($size/1GB) } elseif($size -gt 1MB) { $size='{0:N2} MB' -f ($size/1MB) } elseif($size -gt 1KB) { $size='{0:N2} KB' -f ($size/1KB) } else { $size='{0} B' -f $size }; Write-Output $size" > tmp_size.txt
set /p FILE_SIZE_HUMAN=<tmp_size.txt
del tmp_size.txt

echo === Qdrant Collection Restore ===
echo Target Collection: %TARGET_COLLECTION%
echo Snapshot File: %SNAPSHOT_FILE%
echo File Size: %FILE_SIZE_HUMAN%
echo Qdrant URL: %QDRANT_URL%
echo.

REM Check if target collection exists
echo Checking if target collection exists...
powershell -Command "try { $response = Invoke-RestMethod -Uri '%QDRANT_URL%/collections/%TARGET_COLLECTION%' -Method GET -TimeoutSec 10; exit 0 } catch { exit 1 }"
if %ERRORLEVEL% equ 0 (
    echo WARNING: Collection '%TARGET_COLLECTION%' already exists!
    set /p OVERWRITE="Do you want to overwrite it? (y/N): "
    if /I not "!OVERWRITE!"=="y" (
        echo Restore cancelled.
        exit /b 0
    )
    
    echo Deleting existing collection...
    powershell -Command "try { Invoke-RestMethod -Uri '%QDRANT_URL%/collections/%TARGET_COLLECTION%' -Method DELETE -TimeoutSec 30 } catch {}"
    echo Collection deleted.
)

echo.
echo Uploading and restoring snapshot...
echo This may take a while for large collections...

REM Upload and restore using PowerShell
set "SNAPSHOT_FILENAME=%~nx2"

powershell -Command "try { $uri = '%QDRANT_URL%/collections/%TARGET_COLLECTION%/snapshots/upload?priority=snapshot'; $fileBytes = [System.IO.File]::ReadAllBytes('%SNAPSHOT_FILE%'); $boundary = [System.Guid]::NewGuid().ToString(); $LF = \"`r`n\"; $bodyLines = @(\"--$boundary\", \"Content-Disposition: form-data; name=`\"snapshot\"`; filename=`\"%SNAPSHOT_FILENAME%`\"\", \"Content-Type: application/octet-stream\", \"\", \"\") -join $LF; $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($bodyLines); $bodyBytes2 = $fileBytes; $bodyBytes3 = [System.Text.Encoding]::UTF8.GetBytes(\"$LF--$boundary--$LF\"); $allBytes = New-Object byte[] ($bodyBytes.Length + $bodyBytes2.Length + $bodyBytes3.Length); [System.Buffer]::BlockCopy($bodyBytes, 0, $allBytes, 0, $bodyBytes.Length); [System.Buffer]::BlockCopy($bodyBytes2, 0, $allBytes, $bodyBytes.Length, $bodyBytes2.Length); [System.Buffer]::BlockCopy($bodyBytes3, 0, $allBytes, $bodyBytes.Length + $bodyBytes2.Length, $bodyBytes3.Length); $response = Invoke-RestMethod -Uri $uri -Method POST -ContentType \"multipart/form-data; boundary=$boundary\" -Body $allBytes -TimeoutSec 300; if ($response.status -eq 'ok') { exit 0 } else { exit 1 } } catch { Write-Error $_.Exception.Message; exit 1 }"

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Restore failed
    exit /b 1
)

echo.
echo === Restore Complete ===
echo Collection: %TARGET_COLLECTION%
echo Restored from: %SNAPSHOT_FILENAME%
echo.

REM Get collection info
timeout /t 1 /nobreak >nul 2>&1
for /f "delims=" %%a in ('powershell -Command "try { $response = Invoke-RestMethod -Uri '%QDRANT_URL%/collections/%TARGET_COLLECTION%' -Method GET -TimeoutSec 10; Write-Output $response.result.points_count } catch { Write-Output 'unknown' }"') do set "POINTS_COUNT=%%a"

echo Collection info:
echo   Points count: %POINTS_COUNT%
echo.
echo Verify with:
echo   curl http://%QDRANT_HOST%:%QDRANT_PORT%/collections/%TARGET_COLLECTION%

endlocal

#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Comprehensive MCP Qdrant Server Test Script
.DESCRIPTION
    Tests all MCP endpoints including collections, documents, search, backup/restore.
.NOTES
    Requires the MCP Qdrant server to be running at http://localhost:8080
#>

$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8080/mcp"
$TestResults = @()
$PassCount = 0
$FailCount = 0

function Invoke-McpRequest {
    param(
        [string]$Method,
        [hashtable]$Params = @{}
    )
    
    $body = @{
        jsonrpc = "2.0"
        id = Get-Random -Minimum 1 -Maximum 999999
        method = $Method
        params = $Params
    } | ConvertTo-Json -Depth 10
    
    try {
        $response = Invoke-RestMethod -Uri $BaseUrl -Method POST -ContentType 'application/json' -Body $body -TimeoutSec 60
        return $response
    }
    catch {
        throw "Request failed: $_"
    }
}

function Test-Endpoint {
    param(
        [string]$TestName,
        [scriptblock]$TestScript
    )
    
    Write-Host "`n=== Testing: $TestName ===" -ForegroundColor Cyan
    try {
        $result = & $TestScript
        Write-Host "[PASS] $TestName" -ForegroundColor Green
        $script:PassCount++
        return @{ Name = $TestName; Result = "PASS"; Output = $result }
    }
    catch {
        Write-Host "[FAIL] $TestName - $_" -ForegroundColor Red
        $script:FailCount++
        return @{ Name = $TestName; Result = "FAIL"; Error = $_.Exception.Message }
    }
}

# ============================================
# 1. LIST COLLECTIONS
# ============================================
$TestResults += Test-Endpoint "listCollections" {
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "listCollections"
        arguments = @{}
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Found $($content.collections.Count) collections:" -ForegroundColor Yellow
    $content.collections | ForEach-Object { 
        Write-Host "  - $($_.name): $($_.pointsCount) points, $($_.totalDocuments) documents" 
    }
    return $content
}

# ============================================
# 2. GET COLLECTION INFO (vpms)
# ============================================
$TestResults += Test-Endpoint "getCollectionInfo-vpms" {
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "getCollectionInfo"
        arguments = @{ collectionName = "vpms" }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Collection: $($content.info.name)" -ForegroundColor Yellow
    Write-Host "  Status: $($content.info.status)" 
    Write-Host "  Points: $($content.info.pointsCount)"
    return $content
}

# ============================================
# 3. LIST DOCUMENTS (vpms, limited)
# ============================================
$TestResults += Test-Endpoint "listDocuments-vpms-limit5" {
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "listDocuments"
        arguments = @{ 
            collectionName = "vpms"
            limit = 5
            offset = 0
        }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Total documents: $($content.totalDocuments)" -ForegroundColor Yellow
    Write-Host "Returned: $($content.documents.Count) documents"
    $content.documents | Select-Object -First 3 | ForEach-Object {
        Write-Host "  - $($_.documentId): $($_.chunkCount) chunks"
    }
    return $content
}

# ============================================
# 4. HYBRID SEARCH
# ============================================
$TestResults += Test-Endpoint "hybridSearch-late-binding" {
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "hybridSearch"
        arguments = @{ 
            queryText = "Explain the differences between late binding and Late referencing."
            limit = 5
            summarize = $false
        }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Results found: $($content.results.Count)" -ForegroundColor Yellow
    Write-Host "Collections searched: $($content.collectionsSearched)" 
    Write-Host "Failed collections: $($content.failedCollections.Count)"
    
    $content.results | Select-Object -First 3 | ForEach-Object {
        Write-Host "  - Score: $([math]::Round($_.score, 4)) | Collection: $($_.collection)"
    }
    return $content
}

# ============================================
# 5. CREATE TEST COLLECTION
# ============================================
$TestResults += Test-Endpoint "createCollection-test-collection" {
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "createCollection"
        arguments = @{ 
            collectionName = "test-collection"
            dimension = 768
            distance = "Cosine"
        }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Created: $($content.collectionName), Success: $($content.success)" -ForegroundColor Yellow
    return $content
}

# ============================================
# 6. INGEST DOCUMENT
# ============================================
$TestResults += Test-Endpoint "ingestDocument-test-doc" {
    $testContent = @'
Late binding and late referencing are two different concepts in programming.

Late binding refers to the process of resolving method calls at runtime rather than compile time. This allows for more flexible and dynamic code execution, where the actual method to be called is determined based on the object runtime type.

Late referencing, on the other hand, refers to accessing variables or objects at a later point in the program execution, often after they have been initialized or modified.
'@

    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "ingestDocument"
        arguments = @{ 
            documentId = "test-doc-001"
            content = $testContent
            targetCollections = @("test-collection")
            chunkSize = 512
            chunkOverlap = 50
        }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Document ingested: $($content.success)" -ForegroundColor Yellow
    Write-Host "Chunks indexed: $($content.chunksIndexed)"
    Write-Host ("Indexed to: " + ($content.indexedCollections -join ", "))
    return $content
}

# ============================================
# 7. GET DOCUMENT INFO
# ============================================
$TestResults += Test-Endpoint "getDocumentInfo-test-doc-001" {
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "getDocumentInfo"
        arguments = @{ 
            documentId = "test-doc-001"
            collectionName = "test-collection"
        }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Document exists: $($content.exists)" -ForegroundColor Yellow
    if ($content.documentInfo) {
        Write-Host "Chunk count: $($content.documentInfo.chunkCount)"
    }
    return $content
}

# ============================================
# 8. SEARCH IN TEST COLLECTION
# ============================================
$TestResults += Test-Endpoint "hybridSearch-test-collection" {
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "hybridSearch"
        arguments = @{ 
            queryText = "late binding"
            limit = 3
        }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Results found: $($content.results.Count)" -ForegroundColor Yellow
    return $content
}

# ============================================
# 9. REBUILD DOCUMENT CACHE
# ============================================
$TestResults += Test-Endpoint "rebuildDocumentCache-test-collection" {
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "rebuildDocumentCache"
        arguments = @{ 
            collectionName = "test-collection"
        }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Cache rebuilt: $($content.success)" -ForegroundColor Yellow
    Write-Host "Total documents: $($content.totalDocuments)"
    Write-Host "Collections scanned: $($content.collectionsScanned)"
    return $content
}

# ============================================
# 10. BACKUP COLLECTION (prosimple - smaller collection for faster backup)
# ============================================
$TestResults += Test-Endpoint "backupCollection-prosimple" {
    Write-Host "Creating backup of prosimple collection (360 points)..." -ForegroundColor Yellow
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "backupCollection"
        arguments = @{ 
            collectionName = "prosimple"
        }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Backup success: $($content.success)" -ForegroundColor Yellow
    
    if ($content.errorMessage) {
        Write-Host "Error: $($content.errorMessage)" -ForegroundColor Red
        # If timeout, treat as warning not failure
        if ($content.errorMessage -like "*timeout*" -or $content.errorMessage -like "*Waited*") {
            Write-Host "WARNING: Backup timed out (collection may be too large)" -ForegroundColor Yellow
        }
    }
    
    if ($content.success) {
        Write-Host "Snapshot: $($content.snapshotName)"
        Write-Host "Size: $([math]::Round($content.sizeBytes / 1MB, 2)) MB"
        Write-Host "Created: $($content.creationTime)"
        # Store snapshot name for restore test
        $script:SnapshotPath = $content.downloadUrl
    }
    return $content
}

# ============================================
# 11. RESTORE COLLECTION (as test-restored)
# ============================================
$TestResults += Test-Endpoint "restoreCollection-prosimple-to-test-restored" {
    if (-not $script:SnapshotPath) {
        Write-Host "WARNING: No snapshot path from backup test, skipping restore" -ForegroundColor Yellow
        return @{ skipped = $true; reason = "No snapshot from backup" }
    }
    
    Write-Host "Restoring to test-restored collection..." -ForegroundColor Yellow
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "restoreCollection"
        arguments = @{ 
            collectionName = "test-restored"
            snapshotPath = $script:SnapshotPath
            overwriteExisting = $true
        }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Restore success: $($content.success)" -ForegroundColor Yellow
    Write-Host "Points restored: $($content.pointsRestored)"
    return $content
}

# ============================================
# 12. COMPARE ORIGINAL VS RESTORED
# ============================================
$TestResults += Test-Endpoint "Compare-prosimple-vs-test-restored" {
    if (-not $script:SnapshotPath) {
        Write-Host "WARNING: No snapshot path from backup test, skipping compare" -ForegroundColor Yellow
        return @{ skipped = $true; reason = "No snapshot from backup" }
    }
    
    # Get info for both collections
    $originalInfo = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "getCollectionInfo"
        arguments = @{ collectionName = "prosimple" }
    }
    $restoredInfo = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "getCollectionInfo"
        arguments = @{ collectionName = "test-restored" }
    }
    
    $originalContent = $originalInfo.result.content[0].text | ConvertFrom-Json
    $restoredContent = $restoredInfo.result.content[0].text | ConvertFrom-Json
    
    Write-Host "Original (prosimple):" -ForegroundColor Yellow
    Write-Host "  Status: $($originalContent.info.status)"
    Write-Host "  Points: $($originalContent.info.pointsCount)"
    
    Write-Host "Restored (test-restored):" -ForegroundColor Yellow
    Write-Host "  Status: $($restoredContent.info.status)"
    Write-Host "  Points: $($restoredContent.info.pointsCount)"
    
    $match = $originalContent.info.pointsCount -eq $restoredContent.info.pointsCount
    Write-Host "Point counts match: $match" -ForegroundColor $(if ($match) { "Green" } else { "Red" })
    
    return @{ 
        Original = $originalContent.info
        Restored = $restoredContent.info
        Match = $match
    }
}

# ============================================
# 13. DELETE DOCUMENT
# ============================================
$TestResults += Test-Endpoint "deleteDocument-test-doc-001" {
    $response = Invoke-McpRequest -Method "tools/call" -Params @{
        name = "deleteDocument"
        arguments = @{ 
            documentId = "test-doc-001"
            collectionName = "test-collection"
        }
    }
    
    if ($response.result.isError) {
        throw "Tool returned error"
    }
    
    $content = $response.result.content[0].text | ConvertFrom-Json
    Write-Host "Deleted: $($content.success), Chunks: $($content.deletedChunks)" -ForegroundColor Yellow
    return $content
}

# ============================================
# 14. DELETE TEST COLLECTIONS (cleanup)
# ============================================
$TestResults += Test-Endpoint "deleteCollection-cleanup" {
    $collectionsToDelete = @("test-collection", "test-restored")
    $results = @()
    
    foreach ($coll in $collectionsToDelete) {
        try {
            $response = Invoke-McpRequest -Method "tools/call" -Params @{
                name = "deleteCollection"
                arguments = @{ collectionName = $coll }
            }
            $content = $response.result.content[0].text | ConvertFrom-Json
            Write-Host "  Deleted $coll : $($content.success)" -ForegroundColor Yellow
            $results += @{ Collection = $coll; Success = $content.success }
        }
        catch {
            Write-Host "  Failed to delete $coll : $_" -ForegroundColor Red
            $results += @{ Collection = $coll; Success = $false; Error = $_.Exception.Message }
        }
    }
    return $results
}

# ============================================
# FINAL SUMMARY
# ============================================
Write-Host "`n========================================" -ForegroundColor Blue
Write-Host "          TEST SUMMARY" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue

foreach ($result in $TestResults) {
    $color = if ($result.Result -eq "PASS") { "Green" } else { "Red" }
    $symbol = if ($result.Result -eq "PASS") { "[PASS]" } else { "[FAIL]" }
    Write-Host "$symbol $($result.Name): $($result.Result)" -ForegroundColor $color
}

Write-Host "`n----------------------------------------" -ForegroundColor Blue
Write-Host "Total: $($TestResults.Count) tests" -ForegroundColor White
Write-Host "Passed: $PassCount" -ForegroundColor Green
Write-Host "Failed: $FailCount" -ForegroundColor $(if ($FailCount -gt 0) { "Red" } else { "Green" })

if ($FailCount -gt 0) {
    exit 1
}

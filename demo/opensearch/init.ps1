$ErrorActionPreference = "Stop"

$hostName = $env:OPENSEARCH_HOST
if (-not $hostName) { $hostName = "localhost" }

$port = $env:OPENSEARCH_PORT
if (-not $port) { $port = "9200" }

$index = $env:OPENSEARCH_INDEX
if (-not $index) { $index = "ic_kb_chunk_idx" }

$baseUrl = "http://$hostName`:$port"

Write-Host "Using OpenSearch: $baseUrl"
Write-Host "Index: $index"

# 1) Create index (idempotent: delete if exists, then create)
try {
  Invoke-RestMethod -Method Delete -Uri "$baseUrl/$index" | Out-Null
  Write-Host "Deleted existing index $index"
} catch {
  Write-Host "Index not found (ok): $index"
}

$mapping = @"
{
  "settings": {
    "analysis": {
      "analyzer": {
        "cn_ngram_analyzer": {
          "tokenizer": "standard"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "kbId": { "type": "keyword" },
      "fileId": { "type": "keyword" },
      "chunkId": { "type": "keyword" },
      "deletedFlag": { "type": "integer" },
      "content": { "type": "text", "analyzer": "cn_ngram_analyzer" }
    }
  }
}
"@

Invoke-RestMethod -Method Put -Uri "$baseUrl/$index" -ContentType "application/json" -Body $mapping | Out-Null
Write-Host "Created index $index"

# 2) Bulk upsert sample docs
$bulk = @"
{ "index": { "_index": "$index", "_id": "sample-1" } }
{ "kbId": "demo-kb", "fileId": "demo-file", "chunkId": "1", "deletedFlag": 1, "content": "这是一个 KB demo chunk，用于展示 OpenSearch 的检索与高亮。" }
{ "index": { "_index": "$index", "_id": "sample-2" } }
{ "kbId": "demo-kb", "fileId": "demo-file", "chunkId": "2", "deletedFlag": 1, "content": "Index mode 支持 TEXT / VECTOR / HYBRID；本 demo 只演示 TEXT(OpenSearch)。" }
"@

Invoke-RestMethod -Method Post -Uri "$baseUrl/_bulk" -ContentType "application/x-ndjson" -Body $bulk | Out-Null
Invoke-RestMethod -Method Post -Uri "$baseUrl/$index/_refresh" | Out-Null
Write-Host "Inserted sample docs and refreshed index."

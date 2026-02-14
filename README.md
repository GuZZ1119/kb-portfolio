# KB Portfolio (Sanitized)

A **portfolio-friendly Knowledge Base (KB) indexing & retrieval system**, distilled from a real-world enterprise backend project and **fully sanitized** for public sharing.

This repository focuses on **core backend architecture and indexing logic**, rather than product UI or business-specific integrations.

> 🔒 **Sanitized guarantee**  
> This repository contains **no private tokens, IP addresses, credentials, internal URLs, customer code, or proprietary dependencies**.

---

## ✨ Key Features

### Core Pipeline
- File parsing → cleaning → chunking
- Chunk-level persistence with metadata (kb / file / chunk)
- Idempotent re-parse & replace strategy for safe reindexing

### Text Indexing (OpenSearch)
- Bulk upsert for chunk documents
- Keyword search with filters (kbId / fileId)
- Highlighted search results
- Designed for large document collections

### Vector Indexing (Architecture-ready)
- Java backend → Python FastAPI vector service
- File-level vector reindex entrypoint
- Payload size control & safety strategy (TRUNCATE / FAIL)
- Easily extensible to Milvus / FAISS / other vector stores

### Index Mode Abstraction
Supports multiple indexing strategies via a unified concept:

- **TEXT**   → OpenSearch (BM25 / keyword search)
- **VECTOR** → Embedding-based semantic search
- **HYBRID** → Combined text + vector retrieval

### Unified Reindex Entry Points
- Reindex by **file**
- Reindex by **knowledge base**
- Internally dispatched by `indexMode`

This mirrors the **design pattern used in the original production system**.

---

## 🧱 Project Structure (Sanitized)

```text
kb-portfolio/
├─ demo/                    # Local demo (OpenSearch via Docker)
├─ docs/                    # Architecture notes & diagrams
├─ samples/                 # Example requests / responses
├─ src_sanitized/
│  ├─ java/
│  │  └─ application-webadmin/
│  │     ├─ controller/     # KB / Search / Index / Vector APIs
│  │     ├─ service/        # Core domain services
│  │     ├─ dao/            # Persistence layer
│  │     ├─ model/          # Domain models
│  │     ├─ dto/            # Request / response DTOs
│  │     └─ resources/
│  │        └─ mapper/      # MyBatis XML mappers
│  └─ python/
│     └─ itpai/             # FastAPI vector service (sanitized)
├─ .env.example             # Environment variable template
├─ application.yml.example  # Sanitized config example
└─ README.md

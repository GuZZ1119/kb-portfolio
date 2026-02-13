\# KB Portfolio (Sanitized)



A portfolio-friendly Knowledge Base (KB) indexing \& retrieval project.



\*\*Core pipeline\*\*

\- File parsing → cleaning → chunking

\- Text indexing with OpenSearch (bulk upsert + search + highlight)

\- Vector indexing (architecture-ready; optional)

\- Index Mode concept: \*\*TEXT / VECTOR / HYBRID\*\*

\- Unified reindex entrypoints (file/kb) — design pattern used in the original system



> This is a \*\*sanitized\*\* repository: no private tokens, IPs, passwords, customer code, or internal dependencies.



\## Quickstart (Demo: OpenSearch)



\### 1) Start OpenSearch

```bash

cd demo

docker compose up -d




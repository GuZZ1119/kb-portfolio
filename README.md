KB Portfolio (Sanitized)
知识库索引与检索作品集（脱敏版）

A portfolio-friendly Knowledge Base (KB) indexing & retrieval system demo.
This repository demonstrates a document-to-search pipeline with OpenSearch, designed to be clean, reproducible, and runnable on any machine.

这是一个用于作品集展示的知识库（KB）索引与检索 Demo。
本仓库展示 文档 → 解析 → 分块 → 索引 → 检索（高亮） 的完整闭环，
目标是做到 干净、可复现、任意新环境一键跑通。

✅ This repository is sanitized / 本仓库已完成脱敏

No private tokens / IPs / passwords / 不包含任何私密 token / IP / 密码

No internal business code / 不包含任何公司内部业务代码

No environment-specific dependencies / 不依赖特定机器/特定环境路径

🚀 What This Demo Shows / 本 Demo 展示内容
Core Pipeline / 核心流程

File upload / 文件上传

Text extraction & cleaning / 文本解析与清洗

Chunking with overlap / 分块（含 overlap）

Full-text indexing with OpenSearch / 使用 OpenSearch 全文索引

Search with highlighted results / 支持高亮检索结果

Idempotent reindex (index can be rebuilt anytime) / 幂等重建索引（可随时重建）

🧩 Indexing Design / 索引设计思想

Chunk-level indexing / 以 chunk（分块）为粒度建索引

Stable chunk IDs for safe rebuild / chunk ID 稳定，支持安全重建

Clear separation between source files and index data / 源文件与索引数据清晰隔离

Index mode concept (design-level) / 索引模式概念（设计层面）：

TEXT（全文检索）

VECTOR（向量检索）

HYBRID（混合检索）

This demo implements TEXT (OpenSearch) mode.
Vector indexing is supported in the full system design.

本 Demo 实现的是 TEXT（OpenSearch）模式。
向量索引属于完整系统设计的一部分，此仓库以可运行 Demo 为主。

🧰 Requirements / 运行环境

Docker Desktop

Docker Compose v2

本地无需安装 Java / Python / 数据库
不需要任何既有环境或历史依赖

⚡ Quickstart (One-Command Demo) / 一键启动 Demo
1) Start all services / 启动服务

Step / 步骤：
进入 demo 目录，然后启动容器服务。

Command / 命令：
```
cd demo
docker compose up -d --build
```
This will start / 将启动以下服务：

OpenSearch: http://localhost:9200

OpenSearch Dashboards: http://localhost:5601

Demo KB API: http://localhost:8080

Wait about 20–30 seconds for OpenSearch to be ready.
等待约 20–30 秒，OpenSearch 初始化完成即可。

2) Upload a document / 上传文档

Command / 命令：
```
curl -F "file=@demo.txt" http://localhost:8080/upload
```
Response example / 返回示例：
{
"fileId": "69742593-d8a8-450a-a933-78996802aa9d",
"filename": "demo.txt"
}

3) Build / rebuild the index / 构建（或重建）索引

Command / 命令：
```
curl -X POST http://localhost:8080/reindex
```
This step will / 此步骤会：

Parse uploaded files / 解析已上传文件

Clean and split text into chunks / 清洗文本并进行分块（含 overlap）

Bulk upsert chunks into OpenSearch / 批量 upsert 写入 OpenSearch

Refresh index for immediate search / 刷新索引以便立刻可搜

4) Search with highlight / 高亮搜索

Command / 命令：
```
curl "http://localhost:8080/search?q=水
"
```

Example response / 返回示例：
{
"count": 2,
"results": [
{
"filename": "demo.txt",
"chunkIndex": 0,
"highlight": "这里有<em>水</em>。OpenSearch 高亮测试。"
}
]
}

🔄 Reindexing Design / 重建索引设计说明

This demo supports safe and repeatable reindexing.
本 Demo 支持安全、可重复的重建索引（幂等）。

Why reindex? / 为什么需要重建索引

Index can be rebuilt if deleted or corrupted / 索引删除或损坏后可重建

Chunking parameters may change / 分块参数可能调整（chunkSize/overlap）

Supports incremental development & debugging / 支持迭代开发与调试

How it works / 工作机制

Each chunk uses a stable ID: fileId:chunkIndex

Reindexing overwrites existing chunks, does not create duplicates

The operation is idempotent

每个 chunk 使用稳定 ID：fileId:chunkIndex
重建同一文件时覆盖旧数据，不会重复写入
整体操作幂等，可重复执行

🧠 Engineering Highlights / 工程亮点

OpenSearch bulk indexing / OpenSearch 批量写入

Chunk-level search with highlight / chunk 粒度检索 + 高亮

Idempotent index rebuild / 幂等重建索引

Fully containerized demo / 全容器化可运行

Zero local environment dependency / 本地零环境依赖

This repository focuses on clarity, portability, and system design, rather than framework or business complexity.
本仓库强调清晰、可移植、可复现的系统设计，而非框架堆叠或业务复杂度。

📊 OpenSearch Dashboards / 可视化界面

After startup, visit / 启动后访问：
http://localhost:5601

You can inspect / 你可以查看：

Index mappings / 索引 mapping

Indexed chunks / 已写入的 chunk 文档

Query behavior / 查询与高亮效果

📦 Project Structure (Demo) / 项目结构（Demo）
```
kb-portfolio/

demo/

docker-compose.yml

api/

app.py

Dockerfile

requirements.txt

demo.txt

src_sanitized/ (Sanitized Java / Python code, design reference)

README.md
```
🛡️ Notes / 说明

This repository is intended for portfolio/demo usage.
本仓库用于作品集展示与可运行 Demo。

The full system includes VECTOR/HYBRID modes, permissions, job orchestration, etc.
完整系统包含 VECTOR/HYBRID、权限、任务编排等能力；此仓库聚焦 TEXT 可复现闭环。

Contributions and issues are welcome.
欢迎提 issue 或 PR。

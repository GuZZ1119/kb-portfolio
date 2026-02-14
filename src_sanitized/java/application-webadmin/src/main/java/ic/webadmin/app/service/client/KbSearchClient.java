/**
 * OpenSearch 客户端抽象（TEXT_OS 索引读写）。
 *
 * <p>职责：
 * <ul>
 *   <li>bulkUpsert：批量写入/覆盖 chunk 文档（用于重建索引、解析后增量同步）。</li>
 *   <li>deleteByQuery：按条件清理旧索引文档（用于文件级/库级重建前的清旧）。</li>
 *   <li>refresh：刷新索引使写入结果立刻可搜索（对调试/接口即时返回很关键）。</li>
 *   <li>searchChunks：按 keyword + 可选 kbId/fileId filter 查询 chunk，并返回高亮片段/score。</li>
 * </ul>
 *
 * <p>核心约定（建议在实现类/配置里保持一致）：
 * <ul>
 *   <li>文档 _id：强烈建议使用 chunkId 作为 _id（最简单且幂等）。</li>
 *   <li>source 字段至少应包含：chunkId、kbId、fileId、chunkIndex、content、deletedFlag（与 mapping 保持一致）。</li>
 *   <li>过滤条件：通常在 query 中增加 deletedFlag=1，并按需过滤 kbId/fileId。</li>
 *   <li>异常处理：实现层应捕获并记录 OpenSearch 返回 errors=true 的详细失败项，便于排障。</li>
 * </ul>
 *
 * <p>注意：
 * <ul>
 *   <li>deleteByQuery 的 jsonBody 是 DSL 原文，调用方应确保字段名/类型与 mapping 匹配。</li>
 *   <li>searchChunks 的 keyword 在某些 analyzer 下可能不支持“单字中文”命中（你们已确认此限制）。</li>
 * </ul>
 */
public interface KbSearchClient {

    /**
     * 批量 upsert 文档（写入/覆盖）。
     *
     * <p>建议实现：使用 OpenSearch /_bulk NDJSON；逐条 action 为 index 或 update+doc_as_upsert。</p>
     *
     * @param index 索引名称（如 ic_kb_chunk_idx）。
     * @param docs  待写入文档列表（建议 docs.size 控制在合理 batchSize，如 200~1000）。
     */
    void bulkUpsert(String index, List<BulkDoc> docs);
    /**
     * 根据 Query DSL 执行 delete-by-query 清理索引文档。
     *
     * <p>典型用途：
     * <ul>
     *   <li>文件级重建：按 fileId（+ deletedFlag=1）删除旧文档</li>
     *   <li>库级重建：按 kbId（+ deletedFlag=1）删除旧文档</li>
     * </ul>
     *
     * @param index    索引名称。
     * @param jsonBody OpenSearch Query DSL（JSON 字符串原文）。
     */
    void deleteByQuery(String index, String jsonBody);
    /**
     * 刷新索引，使最近写入的数据对搜索可见。
     *
     * <p>说明：
     * <ul>
     *   <li>OpenSearch 默认近实时（refresh interval），不 refresh 也会在一段时间后可见；</li>
     *   <li>但为了“重建后立刻能搜到”，通常在重建/调试场景会显式 refresh。</li>
     * </ul>
     *
     * @param index 索引名称。
     */
    void refresh(String index);

    /**
     * 按关键词搜索 chunk 文档。
     *
     * <p>建议实现口径：
     * <ul>
     *   <li>query：match/multi_match on content（含 analyzer）</li>
     *   <li>filter：term deletedFlag=1 + 可选 kbId/fileId（term 查询 keyword 字段）</li>
     *   <li>highlight：对 content 做高亮，返回首段片段</li>
     *   <li>分页：pageNum 从 1 开始；from=(pageNum-1)*pageSize</li>
     * </ul>
     *
     * @param index    索引名称。
     * @param keyword  搜索关键词（建议至少 2 个字符，避免单字命中问题）。
     * @param kbId     可选：知识库过滤条件。
     * @param fileId   可选：文件过滤条件。
     * @param pageNum  页码（从 1 开始）。
     * @param pageSize 每页大小。
     * @return total + hits（hits 包含 chunkId/kbId/fileId/高亮/score 等）。
     */
    SearchResult searchChunks(String index, String keyword, Long kbId, Long fileId, int pageNum, int pageSize);

    /**
     * bulk 写入文档结构。
     *
     * <p>建议：
     * <ul>
     *   <li>id：使用 chunkId 作为 _id，保证幂等 upsert（同 chunk 覆盖写入）。</li>
     *   <li>source：写入的字段 map，字段名必须与 OpenSearch mapping 一致。</li>
     * </ul>
     */
    @Data
    class BulkDoc {
        private String id;              // _id
        private Map<String, Object> source; // 文档内容
    }

    /**
     * 搜索命中项（DTO）。
     *
     * <p>注意：这里 kbId/fileId/chunkId 使用 String 是为了与 OpenSearch _source 的字符串化一致；
     * 若决定在 source 中存 Long，也可以在实现层统一转换。</p>
     */
    @Data
    class SearchHit {
        private String chunkId;
        private String kbId;
        private String fileId;
        private Integer chunkIndex;
        private String highlight;   // 取 content 的第一段高亮
        private Double score;
    }

    /**
     * 搜索结果（DTO）：total + hits。
     */
    @Data
    class SearchResult {
        private long total;
        private List<SearchHit> hits;
    }
}

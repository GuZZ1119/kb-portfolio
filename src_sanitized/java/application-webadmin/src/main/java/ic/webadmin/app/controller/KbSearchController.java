/**
 * KB 文本检索（OpenSearch）相关接口。
 *
 * <p>说明：
 * <ul>
 *   <li>本 Controller 对应 TEXT_OS（文本索引）检索能力，不涉及向量检索（VECTOR）。</li>
 *   <li>检索数据源为 OpenSearch 索引（默认索引名由配置 kbSearchProperties.opensearch.index 指定）。</li>
 *   <li>返回结构统一：ResponseResult.success(data)，其中 data 包含 dataList + totalCount。</li>
 * </ul>
 *
 * <p>权限/鉴权：由全局 Sa-Token 拦截器控制（此处不显式标注）。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/app/kbSearch")
public class KbSearchController {

    private final KbSearchClient kbSearchClient;
    private final KbChunkIndexService kbChunkIndexService;
    private final KbSearchProperties kbSearchProperties;

    /**
     * 文本检索：按关键词搜索分段（chunk）。
     *
     * <p>入参说明：
     * <ul>
     *   <li>keyword：必填，建议至少 2 个字符（当前分词器对“单字中文”可能无法命中）。</li>
     *   <li>kbId/fileId：可选过滤条件，用于限定到指定知识库/文件范围。</li>
     *   <li>pageNum/pageSize：可选，缺省值分别为 1 / 10。</li>
     * </ul>
     *
     * <p>返回结构：
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "dataList": [...],   // 命中的 chunk 列表（包含高亮片段/元数据，取决于 SearchClient 返回）
     *     "totalCount": 123    // 命中总数
     *   }
     * }
     * </pre>
     *
     * <p>注意：
     * <ul>
     *   <li>该接口只负责查询 OpenSearch，不会回查数据库做二次补全（如果后续需要可在 SearchClient 层扩展）。</li>
     *   <li>索引是否最新取决于解析/重建任务是否已完成。</li>
     * </ul>
     * </p>
     */
    @PostMapping("/chunk")
    public ResponseResult searchChunk(@RequestBody SearchReq req) {
        String index = kbSearchProperties.getOpensearch().getIndex();

        KbSearchClient.SearchResult r = kbSearchClient.searchChunks(
                index,
                req.getKeyword(),
                req.getKbId(),
                req.getFileId(),
                req.getPageNum() == null ? 1 : req.getPageNum(),
                req.getPageSize() == null ? 10 : req.getPageSize()
        );

        Map<String, Object> data = new HashMap<>();
        data.put("dataList", r.getHits());
        data.put("totalCount", r.getTotal());

        return ResponseResult.success(data);
    }

    /**
     * 手动重建（文件级）：将某个 fileId 对应的 chunk 全量重建到 OpenSearch。
     *
     * <p>使用场景：
     * <ul>
     *   <li>调试索引是否写入成功</li>
     *   <li>文件重新解析后，需要强制刷新索引</li>
     * </ul>
     *
     * <p>注意：重建策略/幂等性由 kbChunkIndexService.reindexFile 内部保证
     * （通常是 deleteByQuery 旧数据 + DB 全量查询 + bulk upsert + refresh）。</p>
     */
    @PostMapping("/reindex/file/{fileId}")
    public ResponseResult reindexFile(@PathVariable("fileId") Long fileId) {
        kbChunkIndexService.reindexFile(fileId);
        return ResponseResult.success();
    }

    /**
     * 手动重建（库级）：前端“重建索引”按钮对应接口。
     *
     * <p>功能：对指定 kbId 下的所有文件/分段进行全量重建索引。</p>
     * <p>注意：该操作可能耗时较长；如需进度与异步化，建议后续接入 ic_kb_job 任务化执行。</p>
     */
    @PostMapping("/reindex/kb/{kbId}")
    public ResponseResult reindexKb(@PathVariable("kbId") Long kbId) {
        kbChunkIndexService.reindexKb(kbId);
        return ResponseResult.success();
    }

    /**
     * 文本检索请求体。
     *
     * <p>校验：
     * <ul>
     *   <li>keyword：必填（@NotBlank）</li>
     *   <li>kbId/fileId：可选过滤</li>
     *   <li>pageNum/pageSize：可选（Controller 默认 1/10）</li>
     * </ul>
     */
    @Data
    public static class SearchReq {
        @NotBlank
        private String keyword;
        private Long kbId;
        private Long fileId;
        private Integer pageNum;
        private Integer pageSize;
    }
}

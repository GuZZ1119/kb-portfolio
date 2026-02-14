/**
 * 向量索引文件级重建/写入请求 DTO（Java -> 向量服务/Embedding 服务）。
 *
 * <p>用途：
 * <ul>
 *   <li>用于触发某个文件(fileId)的向量 upsert/reindex。</li>
 *   <li>最小闭环：携带 chunks（chunkId + content）供对方生成 embedding 并写入向量库。</li>
 * </ul>
 *
 * <p>字段来源建议：
 * <ul>
 *   <li>kbId/fileId：来自 ic_kb_file / ic_kb_library 关联字段。</li>
 *   <li>vectorIndexConfig：来自 ic_kb_library.vectorIndexConfig（文本字段，原样透传）。</li>
 *   <li>chunks：来自 ic_kb_chunk（仅取有效记录 deleted_flag=1）。</li>
 * </ul>
 *
 * <p>注意：
 * <ul>
 *   <li>kbId 与 fileId 在一次请求中通常应保持一致（fileId 所属 kbId = kbId）。</li>
 *   <li>vectorIndexConfig 为“配置文本”，不强制 JSON；对接服务若需要结构化可自行解析。</li>
 *   <li>content 建议使用“解析后的清洗文本”（与入 OpenSearch 的 cleanText 保持一致），并注意长度上限。</li>
 * </ul>
 */
@Data
public class KbVectorReindexFileReq {

    private Long kbId;
    private Long fileId;

    /**
     * 向量配置（文本存储，不用 JSON 类型也行，你直接把库里 vectorIndexConfig 原样传过去）
     */
    private String vectorIndexConfig;

    /**
     * 分段数据（最小闭环：只传 chunkId + content）
     */
    private List<ChunkItem> chunks;
    /**
     * 分段项。
     *
     * <p>最小闭环要求：
     * <ul>
     *   <li>chunkId：用于向量库主键/幂等 upsert（推荐以 chunkId 作为唯一键）。</li>
     *   <li>content：用于 embedding 的文本内容（建议已清洗，避免包含无意义控制字符）。</li>
     * </ul>
     *
     * <p>可选扩展：
     * <ul>
     *   <li>chunkIndex：用于调试/回显顺序；写入向量库时可作为元数据字段。</li>
     * </ul>
     */
    @Data
    public static class ChunkItem {
        /** 分段Id（ic_kb_chunk.id），推荐作为向量库唯一键（primary key）。 */
        private Long chunkId;
        /** 分段序号（ic_kb_chunk.chunk_index），用于顺序回显/调试（可选）。 */
        private Integer chunkIndex;
        /** 分段文本内容（用于 embedding）。 */
        private String content;
    }

    // 待实现：索引版本号，Milvus（pymilvus）侧需要有 index_version 字段
    // private Integer indexVersion;
}

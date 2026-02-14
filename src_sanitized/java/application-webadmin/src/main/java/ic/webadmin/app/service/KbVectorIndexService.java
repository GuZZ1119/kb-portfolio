package ic.webadmin.app.service;
/**
 * KB 向量索引服务（VECTOR）。
 *
 * <p>职责：
 * <ul>
 *   <li>以数据库 ic_kb_chunk 的分段文本为数据源，生成 embedding 并写入向量库（如 Milvus）。</li>
 *   <li>提供文件级 upsert 与库级重建能力，供解析完成后的索引同步、手动补偿、调试使用。</li>
 * </ul>
 *
 * <p>核心约定（建议实现遵循，写到实现类注释里更好）：
 * <ul>
 *   <li>数据来源：仅使用 deleted_flag=1 的有效 chunk（建议按 chunkIndex 升序读取，便于排查）。</li>
 *   <li>幂等性：同一 chunkId 重复写入应覆盖（upsert），不产生重复向量。</li>
 *   <li>清旧策略：
 *     <ul>
 *       <li>文件级：通常按 fileId 清旧（delete-by-fileId）后再 upsert 当前 chunks</li>
 *       <li>库级：通常按 kbId 清旧（delete-by-kbId 或遍历 fileId 清旧）后再全量 upsert</li>
 *     </ul>
 *   </li>
 *   <li>配置来源：向量配置（vectorIndexConfig）通常来自 ic_kb_library，并随请求透传给向量服务。</li>
 * </ul>
 *
 * <p>边界/建议：
 * <ul>
 *   <li>向量写入可能耗时（embedding + 网络 IO），数据量大时建议任务化（ic_kb_job）并提供进度与重试。</li>
 *   <li>删除/软删 chunk 后，向量库的同步删除策略需在实现中明确（避免“DB 已删但向量仍命中”）。</li>
 * </ul>
 */
public interface KbVectorIndexService {
    /**
     * 文件级 upsert：将指定 fileId 的全部有效 chunk 写入/覆盖到向量库。
     *
     * <p>典型用途：
     * <ul>
     *   <li>文件解析完成后的自动同步（INDEX_MODE=VECTOR 或 HYBRID）</li>
     *   <li>手动“重建向量索引（文件级）”补偿/调试</li>
     * </ul>
     *
     * <p>建议实现流程：
     * <ol>
     *   <li>校验文件存在且属于有效知识库（deleted_flag=1）</li>
     *   <li>查询该 fileId 的有效 chunk（deleted_flag=1，按 chunkIndex 升序）</li>
     *   <li>读取 kb 的 vectorIndexConfig（原样透传）</li>
     *   <li>调用 KbVectorClient.reindexFile：清旧 + 批量 upsert</li>
     * </ol>
     *
     * @param fileId 文件Id（不能为空）
     */
    void upsertFile(Long fileId);
    /**
     * 库级重建：对指定 kbId 下所有文件的 chunk 执行向量索引全量重建。
     *
     * <p>典型用途：前端“重建索引（库级）”按钮（VECTOR 或 HYBRID 场景）。</p>
     *
     * <p>建议实现方式：
     * <ul>
     *   <li>方式A：一次性按 kbId 清旧 → DB 全量拉取 chunk → 分批 upsert（更复杂但更高效）</li>
     *   <li>方式B（常用）：遍历 kb 下 fileId 列表，逐个调用 upsertFile（实现简单，但耗时较长）</li>
     * </ul>
     *
     * @param kbId 知识库Id（不能为空）
     */
    void reindexKb(Long kbId);
}

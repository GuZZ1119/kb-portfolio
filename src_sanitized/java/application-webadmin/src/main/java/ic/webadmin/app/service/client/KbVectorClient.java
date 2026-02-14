/**
 * 向量索引服务客户端（Java WebAdmin -> 向量/Embedding 服务）。
 *
 * <p>职责：
 * <ul>
 *   <li>将某个文件的 chunk 文本（chunkId + content）发送给向量服务，触发 embedding 生成与向量库写入。</li>
 * </ul>
 *
 * <p>语义约定（建议对齐对接口径）：
 * <ul>
 *   <li>reindexFile：文件级“重建/写入”向量索引。通常实现为：按 fileId 清旧 → 对 chunks 逐条/批量 upsert。</li>
 *   <li>幂等性：同一个 chunkId 重复调用应覆盖写入（upsert），不产生重复向量。</li>
 * </ul>
 *
 * <p>请求字段最小闭环：
 * <ul>
 *   <li>kbId、fileId（用于清旧与路由/分区）</li>
 *   <li>chunks：至少 chunkId + content（可选带 chunkIndex 作为元数据）</li>
 *   <li>vectorIndexConfig：配置文本原样透传（可为 JSON 字符串或其他格式）</li>
 * </ul>
 *
 * <p>异常/失败处理建议：
 * <ul>
 *   <li>客户端应在实现层捕获网络超时/非 2xx，并记录关键上下文（kbId/fileId/chunk 数量/traceId）。</li>
 *   <li>如需任务化：可由上层 KbVectorIndexService 将调用包装为 ic_kb_job，失败可重试并回写进度/原因。</li>
 * </ul>
 */
public interface KbVectorClient {
    /**
     * 文件级向量索引重建/写入。
     *
     * <p>注意：该方法是否同步阻塞取决于实现（HTTP 调用通常为同步）。若数据量大，
     * 建议上层改为任务化执行并返回 jobId。</p>
     *
     * @param req 请求体（包含 kbId/fileId/vectorIndexConfig/chunks）。
     * @return 响应体（建议包含成功/失败、写入条数、错误信息等）。
     */
    KbVectorReindexFileResp reindexFile(KbVectorReindexFileReq req);
}

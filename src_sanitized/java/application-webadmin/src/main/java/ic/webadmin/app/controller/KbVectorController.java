/**
 * KB 向量索引（VECTOR）相关接口。
 *
 * <p>定位：
 * <ul>
 *   <li>该 Controller 负责触发“向量库”侧的写入/重建（通常对应 Milvus/PGVector 等）。</li>
 *   <li>与 kbSearch（OpenSearch/BM25）不同，这里不做文本检索查询，仅做向量索引维护。</li>
 * </ul>
 *
 * <p>权限：
 * <ul>
 *   <li>@SaCheckPermission("kbLibrary.update")：具备知识库配置/更新权限的用户才可触发重建。</li>
 * </ul>
 *
 * <p>日志审计：
 * <ul>
 *   <li>@OperationLog(type=UPDATE)：记录“重建索引”这类高风险操作，便于追踪。</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/admin/app/kbVector")
public class KbVectorController {

    /**
     * 向量索引服务：
     * - upsertFile(fileId)：按文件增量写入（通常会先按 fileId 清旧，再按 chunk 全量 upsert）
     * - reindexKb(kbId)：按库全量重建（遍历库下文件/分段，清旧后批量写入）
     */
    @Autowired
    private KbVectorIndexService kbVectorIndexService;

    /**
     * 手动重建（文件级 / 向量索引）。
     *
     * <p>语义：对指定 fileId 的 chunk 执行向量 upsert（写入/覆盖）。
     * 通常用于：
     * <ul>
     *   <li>文件重新解析后，需要同步向量库</li>
     *   <li>调试向量链路（embedding 是否正常、向量库写入是否成功）</li>
     * </ul>
     *
     * <p>注意：
     * <ul>
     *   <li>该操作可能耗时（embedding + 写入向量库），如需更好的体验建议任务化（ic_kb_job）。</li>
     *   <li>幂等性/是否清旧由 kbVectorIndexService.upsertFile 内部保证。</li>
     * </ul>
     */
    @SaCheckPermission("kbLibrary.update")
    @OperationLog(type = SysOperationLogType.UPDATE)
    @PostMapping("/reindex/file/{fileId}")
    public ResponseResult<Void> reindexFile(@PathVariable Long fileId) {
        if (MyCommonUtil.existBlankArgument(fileId)) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }
        kbVectorIndexService.upsertFile(fileId);
        return ResponseResult.success();
    }

    /**
     * 手动重建（库级 / 向量索引）：对指定 kbId 下所有文件的 chunk 执行向量库全量重建。
     *
     * <p>与文件级不同，库级重建通常包含：
     * <ul>
     *   <li>按 kbId 清理旧向量（delete-by-kb 或按文件循环清理）</li>
     *   <li>遍历库下文件 → 查询 chunk → embedding → 批量写入向量库</li>
     * </ul>
     *
     * <p>注意：库级重建更耗时，建议后续接入任务化执行并返回 jobId 供前端轮询进度。</p>
     */
    @SaCheckPermission("kbLibrary.update")
    @OperationLog(type = SysOperationLogType.UPDATE)
    @PostMapping("/reindex/kb/{kbId}")
    public ResponseResult<Void> reindexKb(@PathVariable Long kbId) {
        if (MyCommonUtil.existBlankArgument(kbId)) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }
        kbVectorIndexService.reindexKb(kbId);
        return ResponseResult.success();
    }
}

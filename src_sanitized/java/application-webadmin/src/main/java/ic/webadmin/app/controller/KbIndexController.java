@Slf4j
@RestController
@RequestMapping("/admin/app/kbIndex")
public class KbIndexController {

    @Autowired private KbLibraryService kbLibraryService;
    @Autowired private KbFileService kbFileService;
    @Autowired private KbChunkIndexService kbChunkIndexService;
    @Autowired private KbVectorIndexService kbVectorIndexService;

    @SaCheckPermission("kbLibrary.update")
    @OperationLog(type = SysOperationLogType.UPDATE)
    @PostMapping("/reindex/file/{fileId}")
    public ResponseResult<Void> reindexFile(@PathVariable Long fileId) {
        if (MyCommonUtil.existBlankArgument(fileId)) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }

        KbFile file = kbFileService.getById(fileId);
        if (file == null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, "文件不存在，请刷新后重试！");
        }

        KbLibrary lib = kbLibraryService.getById(file.getKbId());
        if (lib == null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, "知识库不存在，请刷新后重试！");
        }

        String mode = normalizeIndexMode(lib.getIndexMode());
        dispatchReindexByMode(mode, file.getKbId(), fileId);

        return ResponseResult.success();
    }

    @SaCheckPermission("kbLibrary.update")
    @OperationLog(type = SysOperationLogType.UPDATE)
    @PostMapping("/reindex/kb/{kbId}")
    public ResponseResult<Void> reindexKb(@PathVariable Long kbId) {
        if (MyCommonUtil.existBlankArgument(kbId)) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }

        KbLibrary lib = kbLibraryService.getById(kbId);
        if (lib == null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, "知识库不存在，请刷新后重试！");
        }

        String mode = normalizeIndexMode(lib.getIndexMode());
        // 库级分发：TEXT 用 reindexKb；VECTOR 用 reindexKb
        if ("TEXT_OS".equals(mode)) {
            kbChunkIndexService.reindexKb(kbId);
        } else if ("VECTOR".equals(mode)) {
            kbVectorIndexService.reindexKb(kbId);
        } else if ("HYBRID".equals(mode)) {
            kbChunkIndexService.reindexKb(kbId);
            kbVectorIndexService.reindexKb(kbId);
        } else {
            // 兜底
            kbChunkIndexService.reindexKb(kbId);
        }

        return ResponseResult.success();
    }

    private void dispatchReindexByMode(String mode, Long kbId, Long fileId) {
        log.info("[kb-index] reindex dispatch, mode={}, kbId={}, fileId={}", mode, kbId, fileId);

        if ("TEXT_OS".equals(mode)) {
            kbChunkIndexService.reindexFile(fileId);
            return;
        }
        if ("VECTOR".equals(mode)) {
            kbVectorIndexService.upsertFile(fileId);
            return;
        }
        if ("HYBRID".equals(mode)) {
            kbChunkIndexService.reindexFile(fileId);
            kbVectorIndexService.upsertFile(fileId);
            return;
        }
        // default
        kbChunkIndexService.reindexFile(fileId);
    }

    private String normalizeIndexMode(String m) {
        if (m == null || m.trim().isEmpty()) return "TEXT_OS";
        m = m.trim().toUpperCase(java.util.Locale.ROOT);
        switch (m) {
            case "BM25": return "TEXT_OS";
            case "EMBEDDING": return "VECTOR";
            case "TEXT_OS":
            case "VECTOR":
            case "HYBRID": return m;
            default: return "TEXT_OS";
        }
    }
}

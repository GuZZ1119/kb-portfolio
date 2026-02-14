@Slf4j
@Service
public class KbVectorIndexServiceImpl implements KbVectorIndexService {

    @Autowired
    private KbFileService kbFileService;
    @Autowired
    private KbLibraryService kbLibraryService;
    @Autowired
    private KbChunkMapper kbChunkMapper;
    @Autowired
    private KbVectorClient kbVectorClient;

    /**
     * payload保护：最大chunks数量（默认300）
     */
    @Value("${kb.vector.payload.maxChunks:300}")
    private int maxChunks;

    /**
     * payload保护：最大字符数（默认300000）
     */
    @Value("${kb.vector.payload.maxChars:300000}")
    private long maxChars;

    /**
     * payload保护策略：TRUNCATE 或 FAIL（默认TRUNCATE）
     */
    @Value("${kb.vector.payload.strategy:TRUNCATE}")
    private String payloadStrategy;

    @Override
    public void upsertFile(Long fileId) {
        KbFile file = kbFileService.getById(fileId);
        if (file == null) {
            log.warn("[kb-vector] upsertFile skip, file not found, fileId={}", fileId);
            return;
        }

        // 可选：只对解析成功的文件做向量入库（更符合“重建”的语义）
        // 如果你希望手动触发也允许解析中/失败的文件尝试，可注释掉这段
        if (file.getParseStatus() != null && !"SUCCESS".equalsIgnoreCase(file.getParseStatus())) {
            log.warn("[kb-vector] upsertFile skip, file parseStatus not SUCCESS, fileId={}, kbId={}, parseStatus={}",
                    fileId, file.getKbId(), file.getParseStatus());
            return;
        }

        Long kbId = file.getKbId();
        KbLibrary lib = kbLibraryService.getById(kbId);
        if (lib == null) {
            log.warn("[kb-vector] upsertFile skip, library not found, kbId={}, fileId={}", kbId, fileId);
            return;
        }

        // 1) 查 chunks（有效记录）
        List<KbChunk> chunkList = kbChunkMapper.selectByFileIdActive(fileId);
        if (CollUtil.isEmpty(chunkList)) {
            log.warn("[kb-vector] upsertFile skip, no chunks, fileId={}, kbId={}", fileId, kbId);
            return;
        }

        // 2) 观测统计
        int chunkCount = chunkList.size();
        long totalChars = 0;
        for (KbChunk c : chunkList) {
            if (c.getContent() != null) totalChars += c.getContent().length();
        }
        log.info("[kb-vector] payload stats, kbId={}, fileId={}, chunkCount={}, totalChars={}, maxChunks={}, maxChars={}, strategy={}",
                kbId, fileId, chunkCount, totalChars, maxChunks, maxChars, payloadStrategy);

        // 3) 保护策略：FAIL / TRUNCATE
        if (chunkCount > maxChunks || totalChars > maxChars) {
            if ("FAIL".equalsIgnoreCase(payloadStrategy)) {
                throw new RuntimeException("payload too large, please batch: chunkCount=" + chunkCount + ", totalChars=" + totalChars);
            }

            // TRUNCATE：同时满足 maxChunks + maxChars
            List<KbChunk> trimmed = new ArrayList<>();
            long acc = 0;
            for (KbChunk c : chunkList) {
                if (trimmed.size() >= maxChunks) break;
                String content = c.getContent();
                if (content == null) continue;
                if (acc + content.length() > maxChars) break;
                trimmed.add(c);
                acc += content.length();
            }
            log.warn("[kb-vector] payload truncated, kbId={}, fileId={}, keptChunks={}, keptChars={}",
                    kbId, fileId, trimmed.size(), acc);
            chunkList = trimmed;

            if (CollUtil.isEmpty(chunkList)) {
                throw new RuntimeException("payload truncated to empty, fileId=" + fileId + ", kbId=" + kbId);
            }
        }

        // 4) 组装请求体
        KbVectorReindexFileReq req = new KbVectorReindexFileReq();
        req.setKbId(kbId);
        req.setFileId(fileId);
        req.setVectorIndexConfig(lib.getVectorIndexConfig());

        List<KbVectorReindexFileReq.ChunkItem> items = new ArrayList<>(chunkList.size());
        for (KbChunk c : chunkList) {
            KbVectorReindexFileReq.ChunkItem it = new KbVectorReindexFileReq.ChunkItem();
            it.setChunkId(c.getId());
            it.setChunkIndex(c.getChunkIndex());
            it.setContent(c.getContent());
            items.add(it);
        }
        req.setChunks(items);

        log.info("[kb-vector] upsertFile begin, kbId={}, fileId={}, chunkCount={}", kbId, fileId, items.size());

        // 5) 调 Python
        KbVectorReindexFileResp resp = kbVectorClient.reindexFile(req);
        if (resp == null || resp.getSuccess() == null || !resp.getSuccess()) {
            String msg = (resp == null) ? "python resp null" : resp.getMessage();
            throw new RuntimeException("python reindexFile failed: " + msg);
        }

        log.info("[kb-vector] upsertFile ok, kbId={}, fileId={}, upsertCount={}", kbId, fileId, resp.getUpsertCount());
    }

    @Override
    public void reindexKb(Long kbId) {
        log.info("[kb-vector] reindexKb begin, kbId={}", kbId);

        List<KbFile> fileList = kbFileService.selectByKbIdActive(kbId);
        if (CollUtil.isEmpty(fileList)) {
            log.info("[kb-vector] reindexKb skip, no files, kbId={}", kbId);
            return;
        }

        int total = fileList.size();
        int eligible = 0;

        for (KbFile f : fileList) {
            // 第5步：只处理“可用文件”（解析成功）
            if (f.getParseStatus() != null && !"SUCCESS".equalsIgnoreCase(f.getParseStatus())) {
                continue;
            }
            eligible++;

            try {
                this.upsertFile(f.getId());
            } catch (Exception e) {
                // 最小闭环：不中断，继续处理下一个文件
                log.error("[kb-vector] reindexKb file upsert failed, kbId={}, fileId={}", kbId, f.getId(), e);
            }
        }

        log.info("[kb-vector] reindexKb done, kbId={}, fileTotal={}, eligibleByParseStatus={}", kbId, total, eligible);
    }
}

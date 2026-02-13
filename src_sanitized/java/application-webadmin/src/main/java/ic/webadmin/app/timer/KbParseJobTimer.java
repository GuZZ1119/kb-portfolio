/**
 * KB 文件解析定时任务（PARSE_FILE Job 执行器）。
 *
 * <p>职责：
 * <ul>
 *   <li>每 5 秒扫描待执行任务（KbJob: status=PENDING 且 jobType=PARSE_FILE）。</li>
 *   <li>对目标文件进行：读取 -> Tika 提取文本 -> 清洗 -> 分段 -> chunk 落库（replace） -> 按 INDEX_MODE 同步索引。</li>
 *   <li>同步更新任务表（ic_kb_job）与文件表（ic_kb_file）的状态/进度/消息，供前端展示。</li>
 * </ul>
 *
 * <p>分布式与幂等：
 * <ul>
 *   <li>使用 Redisson 分布式锁保证同一时间只有一个节点执行（LOCK_KEY=KB_PARSE_JOB_TIMER）。</li>
 *   <li>单次执行只处理 1 个任务（避免长任务阻塞导致大量排队；后续可扩展并发）。</li>
 * </ul>
 *
 * <p>进度口径（当前实现）：
 * <ul>
 *   <li>20% 读取文件</li>
 *   <li>40% 提取文本（Tika）</li>
 *   <li>60% 分段切片（chunk split）</li>
 *   <li>85% 落库分段（replaceChunksByFileId）</li>
 *   <li>95% 同步索引（按 INDEX_MODE 分发：TEXT_OS / VECTOR / HYBRID）</li>
 *   <li>100% 解析完成（索引失败会在 message 中提示，可手动重试重建）</li>
 * </ul>
 */
@EnableScheduling
@Component
@Slf4j
public class KbParseJobTimer {

    private static final long SYSTEM_USER_ID = 0L; // 系统用户Id(可按需调整)
    private static final String LOCK_KEY = "KB_PARSE_JOB_TIMER";

    @org.springframework.beans.factory.annotation.Value("${kb.storage.localBaseDir}")
    private String localBaseDir;

    @Autowired
    private KbLibraryService kbLibraryService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private KbJobMapper kbJobMapper;
    @Autowired
    private KbFileMapper kbFileMapper;
    @Autowired
    private KbChunkService kbChunkService;
    @Autowired
    private KbChunkIndexService kbChunkIndexService;
    @Autowired
    private KbVectorIndexService kbVectorIndexService;

    private RLock distLock;

    /** 规范化后的 kb 根目录 */
    private Path baseDir;

    @PostConstruct
    public void init() {
        distLock = redissonClient.getLock(LOCK_KEY);
        baseDir = Paths.get(localBaseDir).toAbsolutePath().normalize();
        log.info("[KB] localBaseDir(baseDir)={}", baseDir);
    }

    @Scheduled(cron = "*/5 * * * * ?")
    public void execute() {
        try {
            if (!distLock.tryLock(0, 30, TimeUnit.SECONDS)) {
                return;
            }

            KbJob filter = new KbJob();
            filter.setStatus("PENDING");
            filter.setJobType("PARSE_FILE");

            List<KbJob> jobList = kbJobMapper.getKbJobList(filter, "id asc");
            if (CollUtil.isEmpty(jobList)) {
                return;
            }

            executeOne(jobList.get(0));

        } catch (Exception e) {
            log.error("Failed to call KbParseJobTimer.execute", e);
        } finally {
            try {
                if (distLock != null && distLock.isHeldByCurrentThread()) {
                    distLock.unlock();
                }
            } catch (Exception ignore) {
            }
        }
    }

    private void executeOne(KbJob job) {
        KbJob originalJob = kbJobMapper.selectById(job.getId());
        if (originalJob == null) return;
        if (!"PENDING".equals(originalJob.getStatus())) return;

        Long fileId = originalJob.getTargetId();
        if (fileId == null) {
            markFailed(originalJob.getId(), null, "targetId为空，无法关联文件");
            return;
        }

        KbFile file = kbFileMapper.selectById(fileId);
        if (file == null) {
            markFailed(originalJob.getId(), fileId, "关联文件不存在 fileId=" + fileId);
            return;
        }

        KbJob running = new KbJob();
        running.setId(originalJob.getId());
        running.setStatus("RUNNING");
        running.setProgress(0);
        running.setMessage(null);
        running.setStartTime(new Date());
        running.setEndTime(null);
        running.setUpdateUserId(SYSTEM_USER_ID);
        kbJobMapper.updateStatusProgressMessageById(running);

        KbFile parsing = new KbFile();
        parsing.setId(file.getId());
        parsing.setParseStatus("PARSING");
        parsing.setParseProgress(0);
        parsing.setParseMessage(null);
        parsing.setParsedTime(null);
        parsing.setUpdateUserId(SYSTEM_USER_ID);
        kbFileMapper.updateParseStatusProgressMessageById(parsing);

        try {
            if (!"LOCAL".equalsIgnoreCase(file.getStorageType())) {
                throw new RuntimeException("当前仅支持本地解析，storageType=" + file.getStorageType());
            }

            // 1) 读取文件（20%）
            updateProgress(originalJob.getId(), file.getId(), 20, "读取文件");

            Path absPath = resolveStoragePath(file.getStoragePath());
            if (!Files.exists(absPath)) {
                throw new RuntimeException("文件不存在: " + absPath);
            }
            log.info("[KB] parsing fileId={}, kbId={}, storagePath={}, absPath={}",
                    file.getId(), file.getKbId(), file.getStoragePath(), absPath);

            // 2) 提取文本（40%）
            updateProgress(originalJob.getId(), file.getId(), 40, "提取文本");

            String raw;
            {
                AutoDetectParser parser = new AutoDetectParser();
                ParseContext context = new ParseContext();
                Metadata metadata = new Metadata();
                ContentHandler handler = new BodyContentHandler(2_000_000);

                try (InputStream in = Files.newInputStream(absPath)) {
                    parser.parse(in, handler, metadata, context);
                }
                raw = handler.toString();
            }

            String text = cleanText(raw);
            if (text.length() < 20) {
                throw new RuntimeException("提取文本过短：可能为扫描PDF无OCR/空文件/文件损坏");
            }

            // 3) 分段切片（60%）
            updateProgress(originalJob.getId(), file.getId(), 60, "分段切片");

            // 按 UTF-8 字节窗口切分，产出 byteStart/byteEnd/contentByteLen
            List<KbChunkPiece> pieces = ByteChunker.splitUtf8ByBytes(text, 1000, 120);
            if (pieces.isEmpty()) {
                throw new RuntimeException("分段结果为空");
            }

            // 4) 落库分段（85%）
            updateProgress(originalJob.getId(), file.getId(), 85, "落库分段");

            // 写入 byteStart/byteEnd/contentByteLen
            kbChunkService.replaceChunksByFileIdPieces(file.getKbId(), file.getId(), pieces, SYSTEM_USER_ID);

            // 4.5) 索引同步（95%）——按 INDEX_MODE 分发
            updateProgress(originalJob.getId(), file.getId(), 95, "同步索引");
            IndexSyncResult syncResult = dispatchIndexSync(file.getId(), file.getKbId());

            // 5) 成功收尾（100%）
            String finalMsg = syncResult.ok
                    ? "解析完成"
                    : ("解析完成(索引失败，可重试)： " + syncResult.errMsg);

            KbJob successJob = new KbJob();
            successJob.setId(originalJob.getId());
            successJob.setStatus("SUCCESS");
            successJob.setProgress(100);
            successJob.setMessage(finalMsg);
            successJob.setEndTime(new Date());
            successJob.setUpdateUserId(SYSTEM_USER_ID);
            kbJobMapper.updateStatusProgressMessageById(successJob);

            KbFile successFile = new KbFile();
            successFile.setId(file.getId());
            successFile.setParseStatus("SUCCESS");
            successFile.setParseProgress(100);
            successFile.setParseMessage(finalMsg);
            successFile.setParsedTime(new Date());
            successFile.setUpdateUserId(SYSTEM_USER_ID);
            kbFileMapper.updateParseStatusProgressMessageById(successFile);

        } catch (Exception e) {
            log.error("Parse job failed, jobId={}, fileId={}", originalJob.getId(), file.getId(), e);
            markFailed(originalJob.getId(), file.getId(), safeErr(e));
        }
    }

    private Path resolveStoragePath(String storagePath) {
        if (storagePath == null || storagePath.trim().isEmpty()) {
            throw new RuntimeException("storagePath为空");
        }

        String sp = storagePath.replace("\\", "/").trim();
        while (sp.startsWith("/")) sp = sp.substring(1);

        Path abs = baseDir.resolve(sp).normalize();
        if (!abs.startsWith(baseDir)) {
            throw new RuntimeException("非法storagePath(疑似路径穿越): " + storagePath);
        }
        return abs;
    }

    private void updateProgress(Long jobId, Long fileId, int progress, String message) {
        KbJob uj = new KbJob();
        uj.setId(jobId);
        uj.setProgress(progress);
        uj.setMessage(message);
        uj.setUpdateUserId(SYSTEM_USER_ID);
        kbJobMapper.updateStatusProgressMessageById(uj);

        KbFile uf = new KbFile();
        uf.setId(fileId);
        uf.setParseProgress(progress);
        uf.setParseMessage(message);
        uf.setUpdateUserId(SYSTEM_USER_ID);
        kbFileMapper.updateParseStatusProgressMessageById(uf);
    }

    private void markFailed(Long jobId, Long fileId, String errMsg) {
        KbJob failed = new KbJob();
        failed.setId(jobId);
        failed.setStatus("FAILED");
        failed.setProgress(0);
        failed.setMessage(errMsg);
        failed.setEndTime(new Date());
        failed.setUpdateUserId(SYSTEM_USER_ID);
        kbJobMapper.updateStatusProgressMessageById(failed);

        if (fileId != null) {
            KbFile f = new KbFile();
            f.setId(fileId);
            f.setParseStatus("FAILED");
            f.setParseProgress(0);
            f.setParseMessage(errMsg);
            f.setParsedTime(null);
            f.setUpdateUserId(SYSTEM_USER_ID);
            kbFileMapper.updateParseStatusProgressMessageById(f);
        }
    }

    private String cleanText(String s) {
        if (s == null) return "";
        s = s.replace("\u0000", "");
        s = s.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }

    /**
     * 旧的字符切分方法：保留不删，
     * 但当前 executeOne 已切换为 ByteChunker。
     */
    private List<String> split(String text, int chunkSize, int overlap) {
        List<String> res = new ArrayList<>();
        if (text == null) return res;

        text = text.trim();
        if (text.isEmpty()) return res;

        int n = text.length();
        int start = 0;

        while (start < n) {
            int end = Math.min(n, start + chunkSize);

            int cut = end;
            for (int i = end; i > start; i--) {
                char c = text.charAt(i - 1);
                if (c == '\n' || c == '。' || c == '.' || c == '!' || c == '?') {
                    cut = i;
                    break;
                }
            }
            if (cut <= start + chunkSize / 2) cut = end;

            String chunk = text.substring(start, cut).trim();
            if (chunk.length() >= 20) res.add(chunk);

            int nextStart = Math.max(0, cut - overlap);
            if (nextStart <= start) nextStart = cut;
            start = nextStart;
        }
        return res;
    }

    // ====================== 索引分发与结果聚合 ======================
    private IndexSyncResult dispatchIndexSync(Long fileId, Long kbId) {
        IndexSyncResult result = IndexSyncResult.ok();

        KbLibrary lib = kbLibraryService.getById(kbId);
        String mode = (lib == null) ? "TEXT_OS" : normalizeIndexMode(lib.getIndexMode());

        log.info("[kb-index] dispatch begin, kbId={}, fileId={}, mode={}", kbId, fileId, mode);

        if ("TEXT_OS".equals(mode) || "HYBRID".equals(mode)) {
            try {
                kbChunkIndexService.reindexFile(fileId);
                log.info("[kb-index] text index sync ok, fileId={}", fileId);
            } catch (Exception e) {
                log.error("[kb-index] text index sync failed, fileId={}", fileId, e);
                result.mergeFail("文本索引失败: " + safeErr(e));
            }
        }

        if ("VECTOR".equals(mode) || "HYBRID".equals(mode)) {
            try {
                kbVectorIndexService.upsertFile(fileId);
                log.info("[kb-index] vector index sync ok, fileId={}", fileId);
            } catch (Exception e) {
                log.error("[kb-index] vector index sync failed, fileId={}", fileId, e);
                result.mergeFail("向量索引失败: " + safeErr(e));
            }
        }

        log.info("[kb-index] dispatch end, kbId={}, fileId={}, mode={}, ok={}, err={}",
                kbId, fileId, mode, result.ok, result.errMsg);

        return result;
    }

    private String normalizeIndexMode(String m) {
        if (m == null || m.trim().isEmpty()) return "TEXT_OS";
        m = m.trim().toUpperCase(Locale.ROOT);

        switch (m) {
            case "BM25":
                return "TEXT_OS";
            case "EMBEDDING":
                return "VECTOR";
            case "TEXT_OS":
            case "VECTOR":
            case "HYBRID":
                return m;
            default:
                return "TEXT_OS";
        }
    }

    private String safeErr(Exception e) {
        if (e == null) return "unknown";
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) msg = e.getClass().getSimpleName();
        if (msg.length() > 500) msg = msg.substring(0, 500);
        return msg;
    }

    private static class IndexSyncResult {
        private boolean ok;
        private String errMsg;

        public static IndexSyncResult ok() {
            IndexSyncResult r = new IndexSyncResult();
            r.ok = true;
            r.errMsg = null;
            return r;
        }

        public void mergeFail(String msg) {
            this.ok = false;
            if (msg == null || msg.trim().isEmpty()) return;
            if (this.errMsg == null || this.errMsg.trim().isEmpty()) {
                this.errMsg = msg;
            } else {
                this.errMsg = this.errMsg + "；" + msg;
            }
        }
    }
}

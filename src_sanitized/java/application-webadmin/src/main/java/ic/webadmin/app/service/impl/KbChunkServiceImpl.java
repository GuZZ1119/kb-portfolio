@Slf4j
@Service
@RequiredArgsConstructor
public class KbChunkServiceImpl implements KbChunkService {

    private final KbChunkMapper kbChunkMapper;
    private final KbSearchClient kbSearchClient;
    private final KbSearchProperties kbSearchProperties;

    @Override
    public void reindexFile(Long fileId) {
        if (fileId == null) throw new MyRuntimeException("fileId不能为空");

        String index = kbSearchProperties.getOpensearch().getIndex();
        if (index == null || index.trim().isEmpty()) {
            throw new MyRuntimeException("kb.opensearch.index 未配置");
        }

        // 只取 deleted_flag=1 的 chunk
        List<KbChunk> chunks = kbChunkMapper.selectByFileIdActive(fileId);
        if (chunks == null || chunks.isEmpty()) {
            log.info("reindexFile: no chunks found, fileId={}", fileId);
            return;
        }

        List<KbSearchClient.BulkDoc> docs = new ArrayList<>(chunks.size());
        for (KbChunk c : chunks) {
            KbSearchClient.BulkDoc d = new KbSearchClient.BulkDoc();
            d.setId(String.valueOf(c.getId()));

            Map<String, Object> src = new HashMap<>();
            src.put("chunkId", String.valueOf(c.getId()));
            src.put("kbId", String.valueOf(c.getKbId()));
            src.put("fileId", String.valueOf(c.getFileId()));
            src.put("chunkIndex", c.getChunkIndex());
            src.put("content", c.getContent());
            src.put("contentHash", c.getContentHash());
            src.put("contentLen", c.getContentLen());
            src.put("deletedFlag", c.getDeletedFlag());
            src.put("createTime", c.getCreateTime());
            src.put("updateTime", c.getUpdateTime());

            d.setSource(src);
            docs.add(d);
        }

        kbSearchClient.bulkUpsert(index, docs);
        log.info("reindexFile success: fileId={}, docs={}", fileId, docs.size());
    }

    @Override
    public void reindexKb(Long kbId) {
        if (kbId == null) throw new MyRuntimeException("kbId不能为空");

        String index = kbSearchProperties.getOpensearch().getIndex();
        if (index == null || index.trim().isEmpty()) {
            throw new MyRuntimeException("kb.opensearch.index 未配置");
        }

        // 0) 清旧：真正“重建”的关键（按 kbId 删）
        kbSearchClient.deleteByQuery(index,
                "{\"query\":{\"bool\":{\"filter\":[{\"term\":{\"kbId\":\"" + kbId + "\"}}]}}}"
        );

        // 1) 查 DB：只取 deleted_flag=1 的 chunk
        List<KbChunk> chunks = kbChunkMapper.selectByKbIdActive(kbId);
        if (CollUtil.isEmpty(chunks)) {
            log.info("reindexKb: no chunks found, kbId={}", kbId);
            kbSearchClient.refresh(index); // 可选：避免删完仍能搜到的刷新延迟
            return;
        }

        // 2) 分批 bulk（避免一次太大）
        int batchSize = 500; // 你可以放配置
        int total = 0;

        for (int i = 0; i < chunks.size(); i += batchSize) {
            List<KbChunk> sub = chunks.subList(i, Math.min(chunks.size(), i + batchSize));
            List<KbSearchClient.BulkDoc> docs = buildDocs(sub);
            kbSearchClient.bulkUpsert(index, docs);
            total += docs.size();
        }

        // 3) refresh：让按钮点完立即可搜（建议）
        kbSearchClient.refresh(index);

        log.info("reindexKb success: kbId={}, docs={}", kbId, total);
    }

    private List<KbSearchClient.BulkDoc> buildDocs(List<KbChunk> chunks) {
        List<KbSearchClient.BulkDoc> docs = new ArrayList<>(chunks.size());
        for (KbChunk c : chunks) {
            KbSearchClient.BulkDoc d = new KbSearchClient.BulkDoc();
            d.setId(String.valueOf(c.getId())); // _id = chunkId（幂等覆盖）

            Map<String, Object> src = new HashMap<>();
            src.put("chunkId", String.valueOf(c.getId()));
            src.put("kbId", String.valueOf(c.getKbId()));
            src.put("fileId", String.valueOf(c.getFileId()));
            src.put("chunkIndex", c.getChunkIndex());
            src.put("content", c.getContent());
            src.put("contentHash", c.getContentHash());
            src.put("contentLen", c.getContentLen());
            src.put("deletedFlag", c.getDeletedFlag());
            src.put("createTime", c.getCreateTime());
            src.put("updateTime", c.getUpdateTime());

            d.setSource(src);
            docs.add(d);
        }
        return docs;
    }

}

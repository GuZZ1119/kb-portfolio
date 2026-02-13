@Service
public class KbChunkService {

    @Autowired
    private KbChunkMapper kbChunkMapper;

    /**
     * 旧接口：按文件维度替换分段数据（replace 语义）。
     *
     * <p>说明：
     * - 该版本只接收 List<String>，无法得到真实 byteStart/byteEnd。
     * - 为了字段完整性：会写入 contentByteLen(UTF-8字节数)，byteStart/byteEnd 置 0。
     * - 想要真正“字节索引”，请使用 replaceChunksByFileIdPieces（接收 KbChunkPiece）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceChunksByFileId(Long kbId, Long fileId, List<String> chunks, Long userId) {
        Date now = new Date();
        kbChunkMapper.softDeleteByFileId(fileId, userId, now);

        List<KbChunk> list = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);

            KbChunk c = new KbChunk();
            c.setCreateUserId(userId);
            c.setCreateTime(now);
            c.setUpdateUserId(userId);
            c.setUpdateTime(now);
            c.setDeletedFlag(1);

            c.setKbId(kbId);
            c.setFileId(fileId);
            c.setChunkIndex(i);

            c.setContent(content);
            c.setContentLen(content == null ? 0 : content.length());
            c.setContentHash(sha256(content));

            // ✅ 新增：字节字段（旧接口没有offset，先填默认值）
            c.setByteStart(0L);
            c.setByteEnd(0L);
            c.setContentByteLen(content == null ? 0 : content.getBytes(StandardCharsets.UTF_8).length);

            list.add(c);
        }
        if (!list.isEmpty()) {
            kbChunkMapper.batchInsert(list);
        }
    }

    /**
     * 新接口：真正的“字节索引”版本（推荐在解析任务里使用）。
     *
     * <p>该版本要求切分阶段已计算好 UTF-8 字节偏移 byteStart/byteEnd。
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceChunksByFileIdPieces(Long kbId, Long fileId, List<KbChunkPiece> pieces, Long userId) {
        Date now = new Date();
        kbChunkMapper.softDeleteByFileId(fileId, userId, now);

        List<KbChunk> list = new ArrayList<>(pieces.size());
        for (KbChunkPiece it : pieces) {
            String content = it.getContent();

            KbChunk c = new KbChunk();
            c.setCreateUserId(userId);
            c.setCreateTime(now);
            c.setUpdateUserId(userId);
            c.setUpdateTime(now);
            c.setDeletedFlag(1);

            c.setKbId(kbId);
            c.setFileId(fileId);

            c.setChunkIndex(it.getChunkIndex());
            c.setContent(content);
            c.setContentLen(content == null ? 0 : content.length());

            // contentHash：优先用piece里的；没有就现场算一个（保证稳定）
            String hash = it.getContentHash();
            if (hash == null || hash.isBlank()) {
                hash = sha256(content);
            }
            c.setContentHash(hash);

            // ✅ 字节索引字段：必须来自切分阶段
            c.setByteStart(it.getByteStart() == null ? 0L : it.getByteStart());
            c.setByteEnd(it.getByteEnd() == null ? 0L : it.getByteEnd());

            Integer byteLen = it.getContentByteLen();
            if (byteLen == null) {
                byteLen = (content == null) ? 0 : content.getBytes(StandardCharsets.UTF_8).length;
            }
            c.setContentByteLen(byteLen);

            list.add(c);
        }

        if (!list.isEmpty()) {
            kbChunkMapper.batchInsert(list);
        }
    }

    /**
     * 计算字符串的 SHA-256 十六进制摘要。
     */
    private String sha256(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((s == null ? "" : s).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

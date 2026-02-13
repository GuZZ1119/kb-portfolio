/**
 * 知识库文件分段实体对象。
 *
 * 说明：
 * - 一个文件解析后会被切分为多个分段(Chunk)，按CHUNK_INDEX从0开始递增。
 * - 分段内容用于后续向量化/检索/问答等能力。
 *
 * @author 小青蛙
 * @date 2026-01-15
 */
@Data
public class KbChunk {

    /**
     * 主键Id。
     */
    private Long id;

    /**
     * 创建用户Id。
     */
    private Long createUserI

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新用户Id。
     */
    private Long updateUserId;

    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 逻辑删除标记字段(1: 正常 0: 已删除)。
     */
    private Integer deletedFlag;

    /**
     * 知识库Id(关联ic_kb_library.id)。
     */
    private Long kbId;

    /**
     * 文件Id(关联ic_kb_file.id)。
     */
    private Long fileId;

    /**
     * 分段序号(从0开始)。
     */
    private Integer chunkIndex;

    /**
     * 分段内容(纯文本)。
     */
    private String content;

    /**
     * 内容长度(字符数)。
     */
    private Integer contentLen;

    /**
     * 内容哈希(SHA-256)，用于幂等/去重/定位变更。
     */
    private String contentHash;

    /**
     * 分段起始字节偏移(UTF-8)。
     */
    private Long byteStart;

    /**
     * 分段结束字节偏移(UTF-8, exclusive)。
     */
    private Long byteEnd;

    /**
     * 分段内容字节长度(UTF-8)。
     */
    private Integer contentByteLen;
}

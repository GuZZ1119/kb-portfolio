/**
 * 知识库文件实体对象。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ic_kb_file")
public class KbFile extends BaseModel {

    /**
     * 主键Id。
     */
    @UniqueVerifyField
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 逻辑删除标记字段(1: 正常 0: 已删除)。
     */
    @TableLogic
    @TableField(value = "deleted_flag")
    private Integer deletedFlag;

    /**
     * 知识库Id(关联ic_kb_library.id)。
     */
    @TableField(value = "KB_ID")
    private Long kbId;

    /**
     * 文件名(列表展示)。
     */
    @TableField(value = "FILE_NAME")
    private String fileName;

    /**
     * 文件大小(字节/列表展示)。
     */
    @TableField(value = "FILE_SIZE")
    private Long fileSize;

    /**
     * 文件扩展名(txt/pdf/doc/md等)。
     */
    @TableField(value = "FILE_EXT")
    private String fileExt;

    /**
     * 文件类型(MIME)。
     */
    @TableField(value = "MIME_TYPE")
    private String mimeType;

    /**
     * 存储类型(LOCAL: 本地 MINIO: 对象存储)。
     */
    @TableField(value = "STORAGE_TYPE")
    private String storageType;

    /**
     * 存储路径(用于预览/下载)。
     */
    @TableField(value = "STORAGE_PATH")
    private String storagePath;

    /**
     * 解析状态(PENDING: 待解析 PARSING: 解析中 SUCCESS: 成功 FAILED: 失败)。
     */
    @TableField(value = "PARSE_STATUS")
    private String parseStatus;

    /**
     * 解析进度(0-100/用于进度条)。
     */
    @TableField(value = "PARSE_PROGRESS")
    private Integer parseProgress;

    /**
     * 解析结果说明(失败原因/提示信息)。
     */
    @TableField(value = "PARSE_MESSAGE")
    private String parseMessage;

    /**
     * 解析完成时间。
     */
    @TableField(value = "PARSED_TIME")
    private Date parsedTime;

    @RelationDict(
            masterIdField = "id",
            slaveModelClass = KbJob.class,
            slaveIdField = "id",
            slaveNameField = "targetId")
    @TableField(exist = false)
    private Map<String, Object> idDictMap;
}

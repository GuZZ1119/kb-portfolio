/**
 * 知识库文件Dto对象。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Data
public class KbFileDto {

    /**
     * 主键Id。
     */
    @NotNull(message = "数据验证失败，主键Id不能为空！", groups = {UpdateGroup.class})
    private Long id;

    /**
     * 知识库Id(关联ic_kb_library.id)。
     */
    // @NotNull(message = "数据验证失败，知识库Id(关联ic_kb_library.id)不能为空！")
    private Long kbId;

    /**
     * 文件名(列表展示)。
     */
    // @NotBlank(message = "数据验证失败，文件名(列表展示)不能为空！")
    private String fileName;

    /**
     * 文件大小(字节/列表展示)。
     */
    // @NotNull(message = "数据验证失败，文件大小(字节/列表展示)不能为空！")
    private Long fileSize;

    /**
     * 文件扩展名(txt/pdf/doc/md等)。
     */
    private String fileExt;

    /**
     * 文件类型(MIME)。
     */
    private String mimeType;

    /**
     * 存储类型(LOCAL: 本地 MINIO: 对象存储)。
     */
    // @NotBlank(message = "数据验证失败，存储类型(LOCAL: 本地 MINIO: 对象存储)不能为空！")
    private String storageType;

    /**
     * 存储路径(用于预览/下载)。
     */
    // @NotBlank(message = "数据验证失败，存储路径(用于预览/下载)不能为空！")
    private String storagePath;

    /**
     * 解析状态(PENDING: 待解析 PARSING: 解析中 SUCCESS: 成功 FAILED: 失败)。
     */
    // @NotBlank(message = "数据验证失败，解析状态(PENDING: 待解析 PARSING: 解析中 SUCCESS: 成功 FAILED: 失败)不能为空！")
    private String parseStatus;

    /**
     * 解析进度(0-100/用于进度条)。
     */
    // @NotNull(message = "数据验证失败，解析进度(0-100/用于进度条)不能为空！")
    private Integer parseProgress;

    /**
     * 解析结果说明(失败原因/提示信息)。
     */
    private String parseMessage;

    /**
     * 解析完成时间。
     */
    private Date parsedTime;
}

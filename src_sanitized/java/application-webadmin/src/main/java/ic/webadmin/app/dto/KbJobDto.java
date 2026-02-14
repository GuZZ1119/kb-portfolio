/**
 * 知识库文件解析进度Dto对象。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Data
public class KbJobDto {

    /**
     * 主键Id。
     */
    @NotNull(message = "数据验证失败，主键Id不能为空！", groups = {UpdateGroup.class})
    private Long id;

    /**
     * 知识库Id(关联ic_kb_library.id)。
     */
    @NotNull(message = "数据验证失败，知识库Id(关联ic_kb_library.id)不能为空！")
    private Long kbId;

    /**
     * 任务类型(REBUILD_INDEX: 重建索引 PARSE_FILE: 解析文件)。
     */
    @NotBlank(message = "数据验证失败，任务类型(REBUILD_INDEX: 重建索引 PARSE_FILE: 解析文件)不能为空！")
    private String jobType;

    /**
     * 关联目标Id(如file_id/为空表示全库任务)。
     */
    private Long targetId;

    /**
     * 任务状态(PENDING: 排队 RUNNING: 运行 SUCCESS: 成功 FAILED: 失败)。
     */
    @NotBlank(message = "数据验证失败，任务状态(PENDING: 排队 RUNNING: 运行 SUCCESS: 成功 FAILED: 失败)不能为空！")
    private String status;

    /**
     * 任务进度(0-100/用于进度条)。
     */
    @NotNull(message = "数据验证失败，任务进度(0-100/用于进度条)不能为空！")
    private Integer progress;

    /**
     * 任务信息(失败原因/运行说明)。
     */
    private String message;

    /**
     * 开始时间。
     */
    private Date startTime;

    /**
     * 结束时间。
     */
    private Date endTime;
}

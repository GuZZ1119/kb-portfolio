/**
 * 知识库文件解析进度实体对象。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ic_kb_job")
public class KbJob extends BaseModel {

    /**
     * 主键Id。
     */
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
     * 任务类型(REBUILD_INDEX: 重建索引 PARSE_FILE: 解析文件)。
     */
    @TableField(value = "JOB_TYPE")
    private String jobType;

    /**
     * 关联目标Id(如file_id/为空表示全库任务)。
     */
    @TableField(value = "TARGET_ID")
    private Long targetId;

    /**
     * 任务状态(PENDING: 排队 RUNNING: 运行 SUCCESS: 成功 FAILED: 失败)。
     */
    @TableField(value = "STATUS")
    private String status;

    /**
     * 任务进度(0-100/用于进度条)。
     */
    @TableField(value = "PROGRESS")
    private Integer progress;

    /**
     * 任务信息(失败原因/运行说明)。
     */
    @TableField(value = "MESSAGE")
    private String message;

    /**
     * 开始时间。
     */
    @TableField(value = "START_TIME")
    private Date startTime;

    /**
     * 结束时间。
     */
    @TableField(value = "END_TIME")
    private Date endTime;
}

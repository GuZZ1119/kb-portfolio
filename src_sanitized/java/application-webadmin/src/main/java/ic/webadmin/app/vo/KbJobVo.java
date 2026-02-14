package ic.webadmin.app.vo;

import ic.common.core.base.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 知识库文件解析进度VO视图对象。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KbJobVo extends BaseVo {

    /**
     * 主键Id。
     */
    private Long id;

    /**
     * 知识库Id(关联ic_kb_library.id)。
     */
    private Long kbId;

    /**
     * 任务类型(REBUILD_INDEX: 重建索引 PARSE_FILE: 解析文件)。
     */
    private String jobType;

    /**
     * 关联目标Id(如file_id/为空表示全库任务)。
     */
    private Long targetId;

    /**
     * 任务状态(PENDING: 排队 RUNNING: 运行 SUCCESS: 成功 FAILED: 失败)。
     */
    private String status;

    /**
     * 任务进度(0-100/用于进度条)。
     */
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

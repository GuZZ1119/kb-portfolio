package ic.webadmin.app.vo;

import ic.common.core.base.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.Map;

/**
 * 知识库文件VO视图对象。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KbFileVo extends BaseVo {

    /**
     * 主键Id。
     */
    private Long id;

    /**
     * 知识库Id(关联ic_kb_library.id)。
     */
    private Long kbId;

    /**
     * 文件名(列表展示)。
     */
    private String fileName;

    /**
     * 文件大小(字节/列表展示)。
     */
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
    private String storageType;

    /**
     * 存储路径(用于预览/下载)。
     */
    private String storagePath;

    /**
     * 解析状态(PENDING: 待解析 PARSING: 解析中 SUCCESS: 成功 FAILED: 失败)。
     */
    private String parseStatus;

    /**
     * 解析进度(0-100/用于进度条)。
     */
    private Integer parseProgress;

    /**
     * 解析结果说明(失败原因/提示信息)。
     */
    private String parseMessage;

    /**
     * 解析完成时间。
     */
    private Date parsedTime;

    /**
     * id 字典关联数据。
     */
    private Map<String, Object> idDictMap;
}

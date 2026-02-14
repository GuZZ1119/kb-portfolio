/**
 * 知识库实体对象。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ic_kb_library")
public class KbLibrary extends BaseModel {

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
     * 知识库名称(用于列表卡片展示)。
     */
    @TableField(value = "LIB_NAME")
    private String libName;

    /**
     * 知识库类型(DOC: 文档型 FAQ: 问答型 MIXED: 混合型)。
     */
    @TableField(value = "KB_TYPE")
    private String kbType;

    /**
     * 知识库描述(用于编辑页展示)。
     */
    @TableField(value = "KB_DESC")
    private String kbDesc;

    /**
     * 文档数(自动统计/卡片展示)。
     */
    @TableField(value = "DOC_COUNT")
    private Integer docCount;

    /**
     * 累计调用次数(自动统计/卡片展示)。
     */
    @TableField(value = "CALL_COUNT")
    private Long callCount;

    /**
     * 索引状态(AVAILABLE: 可用 BUILDING: 构建中 FAILED: 失败 DISABLED: 禁用)。
     */
    @TableField(value = "INDEX_STATUS")
    private String indexStatus;

    /**
     *  索引模式(TEXT_OS: 关键词/ES  VECTOR: 向量  HYBRID: 混合)。
     */
    @TableField(value = "INDEX_MODE")
    private String indexMode;

    /**
     * 索引配置(文本存储/不使用JSON类型)。
     */
    @TableField(value = "CONFIG_TEXT")
    private String configText;

    /**
     * 配置版本(保存配置/重置配置)。
     */
    // @TableField(value = "CONFIG_VERSION")
    // private Integer configVersion;

    /**
     * 向量索引配置(Milvus/Embedding/不使用JSON类型)。
     */
    @TableField(value = "VECTOR_INDEX_CONFIG")
    private String vectorIndexConfig;

    /**
     * 索引配置版本(保存配置/重置配置/重建索引+1)。
     */
    @TableField(value = "INDEX_VERSION")
    private Integer indexVersion;

    /**
     * LIB_NAME / KB_TYPE LIKE搜索字符串。
     */
    @TableField(exist = false)
    private String searchString;

    public void setSearchString(String searchString) {
        this.searchString = MyCommonUtil.replaceSqlWildcard(searchString);
    }
}

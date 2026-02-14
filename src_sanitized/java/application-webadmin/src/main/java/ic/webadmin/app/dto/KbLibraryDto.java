/**
 * 知识库Dto对象。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Data
public class KbLibraryDto {

    /**
     * 主键Id。
     */
    @NotNull(message = "数据验证失败，主键Id不能为空！", groups = {UpdateGroup.class})
    private Long id;

    /**
     * 知识库名称(用于列表卡片展示)。
     * NOTE: 可支持等于操作符的列表数据过滤。
     */
    @NotBlank(message = "数据验证失败，知识库名称(用于列表卡片展示)不能为空！")
    private String libName;

    /**
     * 知识库类型(DOC: 文档型 FAQ: 问答型 MIXED: 混合型)。
     * NOTE: 可支持等于操作符的列表数据过滤。
     */
    @NotBlank(message = "数据验证失败，知识库类型(DOC: 文档型 FAQ: 问答型 MIXED: 混合型)不能为空！")
    private String kbType;

    /**
     * 知识库描述(用于编辑页展示)。
     */
    private String kbDesc;

    /**
     * 文档数(自动统计/卡片展示)。
     */
    private Integer docCount;

    /**
     * 累计调用次数(自动统计/卡片展示)。
     */
    private Long callCount;

    /**
     * 索引状态(AVAILABLE: 可用 BUILDING: 构建中 FAILED: 失败 DISABLED: 禁用)。
     */
    @NotBlank(message = "数据验证失败，索引状态(AVAILABLE: 可用 BUILDING: 构建中 FAILED: 失败 DISABLED: 禁用)不能为空！")
    private String indexStatus;

    /**
     * 索引模式(BM25: 关键词 EMBEDDING: 向量 HYBRID: 混合)。
     */
    private String indexMode;

    /**
     * 索引配置(文本存储/不使用JSON类型)。
     */
    private String configText;

    /**
     * 配置版本(保存配置/重置配置)。
     */
    // @NotNull(message = "数据验证失败，配置版本(保存配置/重置配置)不能为空！")
    // private Integer configVersion;

    /**
     * 向量索引配置(Milvus/Embedding/不使用JSON类型)。
     */
    private String vectorIndexConfig;

    /**
     * 索引配置版本(保存配置/重置配置/重建索引+1)。
     * NOTE: 一般不要求前端传；由后端维护自增。
     */
    private Integer indexVersion;


    /**
     * LIB_NAME / KB_TYPE LIKE搜索字符串。
     * NOTE: 可支持LIKE操作符的列表数据过滤。
     */
    private String searchString;
}

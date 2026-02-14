/**
 * 知识库数据操作访问接口。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
public interface KbLibraryMapper extends BaseDaoMapper<KbLibrary> {

    /**
     * 批量插入对象列表。
     *
     * @param kbLibraryList 新增对象列表。
     */
    void insertList(List<KbLibrary> kbLibraryList);

    /**
     * 保存/重置索引配置：更新索引模式/配置，同时索引版本+1并将索引状态置为DISABLED。
     */
    int updateIndexConfigById(KbLibrary kbLibrary);

    /**
     * 获取过滤后的对象列表。
     *
     * @param kbLibraryFilter 主表过滤对象。
     * @param orderBy 排序字符串，order by从句的参数。
     * @return 对象列表。
     */
    List<KbLibrary> getKbLibraryList(
            @Param("kbLibraryFilter") KbLibrary kbLibraryFilter, @Param("orderBy") String orderBy);
}

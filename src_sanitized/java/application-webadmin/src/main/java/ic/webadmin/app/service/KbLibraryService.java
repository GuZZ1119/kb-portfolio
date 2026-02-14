/**
 * 知识库数据操作服务接口。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
public interface KbLibraryService extends IBaseService<KbLibrary, Long> {

    /**
     * 保存新增对象。
     *
     * @param kbLibrary 新增对象。
     * @return 返回新增对象。
     */
    KbLibrary saveNew(KbLibrary kbLibrary);

    /**
     * 利用数据库的insertList语法，批量插入对象列表。
     *
     * @param kbLibraryList 新增对象列表。
     */
    void saveNewBatch(List<KbLibrary> kbLibraryList);

    /**
     * 批量插入或更新数据，同时还会根据指定列的数据是否存在进行校验，如果存在则更新，否则批量新增。
     *
     * @param kbLibraryList 数据列表。
     * @param duplicateVerifyColumns 判断指定列的数据是否存在。
     * @param batchSize 每个小批次的数量，小于等于0时将使用默认值。
     */
    void saveNewOrUpdateBatch(List<KbLibrary> kbLibraryList, List<String> duplicateVerifyColumns, int batchSize);

    /**
     * 更新数据对象。
     *
     * @param kbLibrary         更新的对象。
     * @param originalKbLibrary 原有数据对象。
     * @return 成功返回true，否则false。
     */
    boolean update(KbLibrary kbLibrary, KbLibrary originalKbLibrary);

    /**
     * 删除指定数据。
     *
     * @param id 主键Id。
     * @return 成功返回true，否则false。
     */
    boolean remove(Long id);

    /**
     * 获取单表查询结果。由于没有关联数据查询，因此在仅仅获取单表数据的场景下，效率更高。
     * 如果需要同时获取关联数据，请移步(getKbLibraryListWithRelation)方法。
     *
     * @param filter  过滤对象。
     * @param orderBy 排序参数。
     * @return 查询结果集。
     */
    List<KbLibrary> getKbLibraryList(KbLibrary filter, String orderBy);

    /**
     * 保存索引配置（索引版本+1，索引状态置DISABLED）。
     */
    void saveIndexConfig(Long kbId, String indexMode, String configText, String vectorIndexConfig);

    /**
     * 重置索引配置（索引版本+1，索引状态置DISABLED）。
     */
    void resetIndexConfig(Long kbId);

    /**
     * 获取主表的查询结果，以及主表关联的字典数据和一对一从表数据，以及一对一从表的字典数据。
     * 该查询会涉及到一对一从表的关联过滤，或一对多从表的嵌套关联过滤，因此性能不如单表过滤。
     * 如果仅仅需要获取主表数据，请移步(getKbLibraryList)，以便获取更好的查询性能。
     *
     * @param filter 主表过滤对象。
     * @param orderBy 排序参数。
     * @return 查询结果集。
     */
    List<KbLibrary> getKbLibraryListWithRelation(KbLibrary filter, String orderBy);
}

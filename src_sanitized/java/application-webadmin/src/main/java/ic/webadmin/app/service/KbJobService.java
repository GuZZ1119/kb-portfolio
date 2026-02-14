/**
 * 知识库文件解析进度数据操作服务接口。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
public interface KbJobService extends IBaseService<KbJob, Long> {

    /**
     * 保存新增对象。
     *
     * @param kbJob 新增对象。
     * @return 返回新增对象。
     */
    KbJob saveNew(KbJob kbJob);

    /**
     * 利用数据库的insertList语法，批量插入对象列表。
     *
     * @param kbJobList 新增对象列表。
     */
    void saveNewBatch(List<KbJob> kbJobList);

    /**
     * 批量插入或更新数据，同时还会根据指定列的数据是否存在进行校验，如果存在则更新，否则批量新增。
     *
     * @param kbJobList 数据列表。
     * @param duplicateVerifyColumns 判断指定列的数据是否存在。
     * @param batchSize 每个小批次的数量，小于等于0时将使用默认值。
     */
    void saveNewOrUpdateBatch(List<KbJob> kbJobList, List<String> duplicateVerifyColumns, int batchSize);

    /**
     * 更新数据对象。
     *
     * @param kbJob         更新的对象。
     * @param originalKbJob 原有数据对象。
     * @return 成功返回true，否则false。
     */
    boolean update(KbJob kbJob, KbJob originalKbJob);

    /**
     * 删除指定数据。
     *
     * @param id 主键Id。
     * @return 成功返回true，否则false。
     */
    boolean remove(Long id);

    /**
     * 获取单表查询结果。由于没有关联数据查询，因此在仅仅获取单表数据的场景下，效率更高。
     * 如果需要同时获取关联数据，请移步(getKbJobListWithRelation)方法。
     *
     * @param filter  过滤对象。
     * @param orderBy 排序参数。
     * @return 查询结果集。
     */
    List<KbJob> getKbJobList(KbJob filter, String orderBy);

    /**
     * 获取主表的查询结果，以及主表关联的字典数据和一对一从表数据，以及一对一从表的字典数据。
     * 该查询会涉及到一对一从表的关联过滤，或一对多从表的嵌套关联过滤，因此性能不如单表过滤。
     * 如果仅仅需要获取主表数据，请移步(getKbJobList)，以便获取更好的查询性能。
     *
     * @param filter 主表过滤对象。
     * @param orderBy 排序参数。
     * @return 查询结果集。
     */
    List<KbJob> getKbJobListWithRelation(KbJob filter, String orderBy);
}

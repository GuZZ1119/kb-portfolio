/**
 * 知识库文件数据操作服务接口。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
public interface KbFileService extends IBaseService<KbFile, Long> {

    /**
     * 保存新增对象。
     *
     * @param kbFile 新增对象。
     * @return 返回新增对象。
     */
    KbFile saveNew(KbFile kbFile);

    /**
     * 利用数据库的insertList语法，批量插入对象列表。
     *
     * @param kbFileList 新增对象列表。
     */
    void saveNewBatch(List<KbFile> kbFileList);

    /**
     * 批量插入或更新数据，同时还会根据指定列的数据是否存在进行校验，如果存在则更新，否则批量新增。
     *
     * @param kbFileList 数据列表。
     * @param duplicateVerifyColumns 判断指定列的数据是否存在。
     * @param batchSize 每个小批次的数量，小于等于0时将使用默认值。
     */
    void saveNewOrUpdateBatch(List<KbFile> kbFileList, List<String> duplicateVerifyColumns, int batchSize);

    /**
     * 更新数据对象。
     *
     * @param kbFile         更新的对象。
     * @param originalKbFile 原有数据对象。
     * @return 成功返回true，否则false。
     */
    boolean update(KbFile kbFile, KbFile originalKbFile);

    /**
     * 删除指定数据。
     *
     * @param id 主键Id。
     * @return 成功返回true，否则false。
     */
    boolean remove(Long id);

    /**
     * 获取单表查询结果。由于没有关联数据查询，因此在仅仅获取单表数据的场景下，效率更高。
     * 如果需要同时获取关联数据，请移步(getKbFileListWithRelation)方法。
     *
     * @param filter  过滤对象。
     * @param orderBy 排序参数。
     * @return 查询结果集。
     */
    List<KbFile> getKbFileList(KbFile filter, String orderBy);

    /**
     * 获取主表的查询结果，以及主表关联的字典数据和一对一从表数据，以及一对一从表的字典数据。
     * 该查询会涉及到一对一从表的关联过滤，或一对多从表的嵌套关联过滤，因此性能不如单表过滤。
     * 如果仅仅需要获取主表数据，请移步(getKbFileList)，以便获取更好的查询性能。
     *
     * @param filter 主表过滤对象。
     * @param orderBy 排序参数。
     * @return 查询结果集。
     */
    List<KbFile> getKbFileListWithRelation(KbFile filter, String orderBy);

    /**
     * 对批量导入数据列表进行数据合法性验证。
     * 验证逻辑主要覆盖主表的常量字典字段、字典表字典字段、数据源字段和一对一关联数据是否存在。
     *
     * @param dataList 主表的数据列表。
     * @param ignoreFieldSet 需要忽略校验的字典字段集合。通常对于字典反向翻译过来的字段适用，
     *                       避免了二次验证，以提升效率。
     * @return 验证结果。如果失败，包含具体的错误信息和导致错误的数据对象。
     */
    CallResult verifyImportList(List<KbFile> dataList, Set<String> ignoreFieldSet);

    /**
     * 查询指定知识库下的有效文件（deleted_flag=1）。
     *
     * <p>用途：
     * <ul>
     *   <li>库级重建索引时遍历 file 列表（TEXT_OS / VECTOR）</li>
     *   <li>后台任务补偿：扫描库下文件状态</li>
     * </ul>
     *
     * @param kbId 知识库Id。
     * @return 有效文件列表。
     */
    List<KbFile> selectByKbIdActive(Long kbId);
}

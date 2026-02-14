/**
 * 知识库文件数据操作访问接口。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
public interface KbFileMapper extends BaseDaoMapper<KbFile> {

    /**
     * 批量插入对象列表。
     *
     * @param kbFileList 新增对象列表。
     */
    void insertList(List<KbFile> kbFileList);

    /**
     * 获取过滤后的对象列表。
     *
     * @param kbFileFilter 主表过滤对象。
     * @param orderBy 排序字符串，order by从句的参数。
     * @return 对象列表。
     */
    List<KbFile> getKbFileList(
            @Param("kbFileFilter") KbFile kbFileFilter, @Param("orderBy") String orderBy);

    /**
     * 按主键更新解析状态/进度/消息（部分字段更新）。
     *
     * <p>说明：
     * <ul>
     *   <li>通常用于解析任务定时器更新：parseStatus / parseProgress / parseMessage / parsedTime 等。</li>
     *   <li>若 XML 使用 &lt;set&gt; + &lt;if test="field != null"&gt;，则传 null 不会覆盖原值；
     *       如需“清空消息”等行为，需要明确是否允许传空串/允许置 NULL。</li>
     * </ul>
     *
     * @param file 包含 id 以及需要更新的字段（其余字段可为空）。
     * @return 受影响行数（1 表示更新成功，0 表示未找到或已被删除）。
     */
    int updateParseStatusProgressMessageById(KbFile file);

    /**
     * 查询指定知识库下的有效文件列表（仅 deleted_flag=1）。
     *
     * <p>用途：库级重建索引/向量重建时遍历 kb 下文件。</p>
     *
     * @param kbId 知识库Id。
     * @return 文件列表（有效记录）。
     */
    List<KbFile> selectByKbIdActive(@Param("kbId") Long kbId);
}

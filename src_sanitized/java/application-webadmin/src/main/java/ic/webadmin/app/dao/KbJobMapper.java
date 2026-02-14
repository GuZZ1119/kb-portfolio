/**
 * 知识库文件解析进度数据操作访问接口。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
public interface KbJobMapper extends BaseDaoMapper<KbJob> {

    /**
     * 批量插入对象列表。
     *
     * @param kbJobList 新增对象列表。
     */
    void insertList(List<KbJob> kbJobList);

    /**
     * 获取过滤后的对象列表。
     *
     * @param kbJobFilter 主表过滤对象。
     * @param orderBy 排序字符串，order by从句的参数。
     * @return 对象列表。
     */
    List<KbJob> getKbJobList(
            @Param("kbJobFilter") KbJob kbJobFilter, @Param("orderBy") String orderBy);

    /**
     * 按主键更新任务状态/进度/消息（部分字段更新）。
     *
     * <p>典型字段口径（建议在 Service/Enum 注释里也统一）：
     * <ul>
     *   <li>status：PENDING / RUNNING / SUCCESS / FAILED（或你们系统定义的状态集合）</li>
     *   <li>progress：0~100（建议明确每个阶段对应百分比，例如解析/索引同步占比）</li>
     *   <li>message：失败原因或进度描述（如 “解析中(60%)”、“索引同步中” 等）</li>
     * </ul>
     *
     * <p>注意：
     * <ul>
     *   <li>若 XML 使用 &lt;set&gt; + &lt;if test="field != null"&gt;，则传 null 不会覆盖原值；
     *       如需“清空 message”等行为，需要明确是否允许传空串/允许置 NULL。</li>
     * </ul>
     *
     * @param job 包含 id 以及需要更新的字段（其余字段可为空）。
     * @return 受影响行数（1 表示更新成功，0 表示未找到或已被删除）。
     */
    int updateStatusProgressMessageById(KbJob job);
}

/**
 * 知识库文件分段数据操作访问接口。
 *
 * 说明：
 * - 分段数据用于后续向量化入库、检索命中回溯、展示原文片段等。
 * - replace 逻辑通常为：先按 fileId 软删旧分段，再批量插入新分段。
 *
 * @author 小青蛙
 * @date 2026-01-15
 */
public interface KbChunkMapper {

    /**
     * 根据文件Id软删除该文件下所有分段记录。
     *
     * @param fileId       文件Id。
     * @param updateUserId 更新用户Id。
     * @param updateTime   更新时间。
     * @return 受影响行数。
     */
    int softDeleteByFileId(@Param("fileId") Long fileId,
                           @Param("updateUserId") Long updateUserId,
                           @Param("updateTime") Date updateTime);

    /**
     * 批量插入分段对象列表。
     *
     * @param list 分段对象列表。
     * @return 插入条数。
     */
    int batchInsert(@Param("list") List<KbChunk> list);

    /**
     * 根据文件Id获取该文件下所有分段记录列表（仅查询deleted_flag=1）。
     *
     * @param fileId 文件Id。
     * @return 分段列表。
     */

    /**
     * 查询指定文件下的有效 chunk（仅 deleted_flag=1）。
     *
     * <p>建议：
     * <ul>
     *   <li>返回结果应按 chunkIndex 升序（在 XML 中 ORDER BY chunk_index ASC），保证回显与重建索引稳定。</li>
     * </ul>
     *
     * @param fileId 文件Id。
     * @return chunk 列表（有效记录）。
     */
    List<KbChunk> selectByFileIdActive(@Param("fileId") Long fileId);

    /**
     * 查询指定知识库下的有效 chunk（仅 deleted_flag=1）。
     *
     * <p>用途：库级重建索引/向量重建时按 kbId 全量拉取。</p>
     *
     * <p>注意：
     * <ul>
     *   <li>数据量可能较大，建议在 Service 层分页/分批查询（或按 fileId 分批）。</li>
     * </ul>
     *
     * @param kbId 知识库Id。
     * @return chunk 列表（有效记录）。
     */
    List<KbChunk> selectByKbIdActive(@Param("kbId") Long kbId);
}

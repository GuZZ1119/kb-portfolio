public interface KbChunkIndexService {

    /** 解析完成后：按 fileId 重建该文件的 chunk 索引 */
    void reindexFile(Long fileId);

    void reindexKb(Long kbId);        //（库级）
}

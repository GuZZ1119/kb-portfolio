@Data
public class KbVectorReindexFileResp {

    /**
     * Python 返回是否成功
     */
    private Boolean success;

    /**
     * 错误信息（失败时）
     */
    private String message;

    /**
     * 写入/更新的向量数量（可选）
     */
    private Integer upsertCount;
}

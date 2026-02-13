@Data
public class KbChunkPiece {
    private Integer chunkIndex;
    private String content;

    /** UTF-8 byte offset */
    private Long byteStart;
    /** UTF-8 byte offset, exclusive */
    private Long byteEnd;

    private Integer contentByteLen;
    private String contentHash;
}

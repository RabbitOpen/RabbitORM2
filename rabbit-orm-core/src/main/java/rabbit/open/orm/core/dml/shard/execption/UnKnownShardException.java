package rabbit.open.orm.core.dml.shard.execption;

/**
 * <b>Description  无法确认切片异常</b>
 */
@SuppressWarnings("serial")
public class UnKnownShardException extends RuntimeException {

    public UnKnownShardException(String msg) {
        super(msg);
    }
}

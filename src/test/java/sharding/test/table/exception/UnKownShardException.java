package sharding.test.table.exception;

/**
 * <b>Description  无法确认切片异常</b>
 */
@SuppressWarnings("serial")
public class UnKownShardException extends RuntimeException {

    public UnKownShardException(String msg) {
        super(msg);
    }
}

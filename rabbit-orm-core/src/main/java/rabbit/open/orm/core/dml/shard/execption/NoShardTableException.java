package rabbit.open.orm.core.dml.shard.execption;

/**
 * <b>Description  没有找到分区表异常</b>
 */
@SuppressWarnings("serial")
public class NoShardTableException extends RuntimeException {

    public NoShardTableException(Class<?> clz) {
        super("no shard table exception [" + clz + "]");
    }
}

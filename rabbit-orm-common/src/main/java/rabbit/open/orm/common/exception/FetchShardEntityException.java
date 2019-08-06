package rabbit.open.orm.common.exception;

/**
 * <b>Description fetch 获取 joinFetch 分区表时抛出的异常</b>
 */
@SuppressWarnings("serial")
public class FetchShardEntityException extends RuntimeException {

	public FetchShardEntityException(Class<?> clz) {
		super("invalid fetch/join fetch operation with [" + clz + "]");
	}

}

package rabbit.open.orm.exception;

/**
 * <b>Description 非法的join fetch 操作</b>.
 */
@SuppressWarnings("serial")
public class InvalidJoinFetchOperationException extends RabbitDMLException {

	public InvalidJoinFetchOperationException(Class<?> fetch, Class<?> target) {
		super("invalid join fetch operation on [" + fetch.getName()
				+ "] from [" + target.getName() + "]");
	}

}

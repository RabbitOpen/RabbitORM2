package rabbit.open.common.exception;

/**
 * <b>Description 非法的join filter</b>.
 */
@SuppressWarnings("serial")
public class InvalidJoinFilterException extends RabbitDMLException {

	public InvalidJoinFilterException(Class<?> target, Class<?> src) {
		super("[" + target.getName() + "] can't be joined by [" + src.getName()
				+ "]");
	}

}

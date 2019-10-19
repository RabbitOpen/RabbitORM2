package rabbit.open.orm.common.exception;

/**
 * <b>Description 非法的join merge 操作</b>.
 */
@SuppressWarnings("serial")
public class InvalidJoinMergeException extends RabbitDMLException {

	public InvalidJoinMergeException(Class<?> target, Class<?> joinClass) {
		super("invalid merge operation from [" + target + "] to [" + joinClass + "]");
	}

}

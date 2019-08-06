package rabbit.open.common.exception;

/**
 * <b>Description 非法查询路径, 比如循环包含</b>
 */
@SuppressWarnings("serial")
public class InvalidQueryPathException extends RabbitDMLException {

	public InvalidQueryPathException(Class<?> clz) {
		super("invalid query path can be specified to [" + clz.getName() + "]");
	}

}

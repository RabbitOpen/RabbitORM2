package rabbit.open.orm.common.exception;

/**
 * <b>Description jdbc命名查询返回值类型异常 </b>
 */
@SuppressWarnings("serial")
public class InvalidReturnTypeException extends RuntimeException {

	public InvalidReturnTypeException(String queryName) {
		super("invalid return type is found with jdbc sql[" + queryName + "]");
	}

}

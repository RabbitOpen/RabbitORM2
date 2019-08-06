package rabbit.open.common.exception;

/**
 * <b>Description 不匹配的命名查询异常 </b>.
 */
@SuppressWarnings("serial")
public class MisMatchedNamedQueryException extends RuntimeException {

	public MisMatchedNamedQueryException(String name) {
		super("[" + name + "] is defined incorrectly");
	}

}

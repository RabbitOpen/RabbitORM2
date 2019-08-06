package rabbit.open.orm.common.exception;

/**
 * <b>Description 重复别名 </b>.
 */
@SuppressWarnings("serial")
public class RepeatedAliasException extends RuntimeException {

	public RepeatedAliasException(String alias) {
		super("repeated alias[" + alias + "] is defined");
	}

}

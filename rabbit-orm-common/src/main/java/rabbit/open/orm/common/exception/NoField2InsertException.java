package rabbit.open.orm.common.exception;

/**
 * <b>Description 没有任何需要插入的字段异常</b>
 */
@SuppressWarnings("serial")
public class NoField2InsertException extends RabbitDMLException {

	public NoField2InsertException() {
		super("no field to insert");
	}

}

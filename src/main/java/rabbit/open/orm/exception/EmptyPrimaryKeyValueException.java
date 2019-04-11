package rabbit.open.orm.exception;

/**
 * <b>Description 操作中间表时主对象主键字段为空异常</b>
 */
@SuppressWarnings("serial")
public class EmptyPrimaryKeyValueException extends RabbitDMLException {

	public EmptyPrimaryKeyValueException() {
		super("primary key field is empty!");
	}

}

package rabbit.open.common.exception;

/**
 * <b>@description 不能以动态字段进行group by </b>
 */
@SuppressWarnings("serial")
public class InvalidGroupByFieldException extends RabbitDMLException {

	public InvalidGroupByFieldException(String field) {
		super("group by dynamic field[" + field + "] is forbidden!");
	}

}

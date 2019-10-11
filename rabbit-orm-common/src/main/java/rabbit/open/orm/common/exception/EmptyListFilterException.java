package rabbit.open.orm.common.exception;

@SuppressWarnings("serial")
public class EmptyListFilterException extends RabbitDMLException {

	public EmptyListFilterException() {
		super("filter list size can't be empty");
	}

}

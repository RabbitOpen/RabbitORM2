package rabbit.open.orm.exception;

@SuppressWarnings("serial")
public class EmptyListFilterException extends RabbitDMLException {

	public EmptyListFilterException(String message) {
		super(message);
	}

}

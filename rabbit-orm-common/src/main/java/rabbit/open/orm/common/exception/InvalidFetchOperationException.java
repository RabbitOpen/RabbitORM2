package rabbit.open.orm.common.exception;

@SuppressWarnings("serial")
public class InvalidFetchOperationException extends RabbitDMLException {

	public InvalidFetchOperationException(String message) {
		super(message);
	}

}

package rabbit.open.common.exception;

@SuppressWarnings("serial")
public class UnKnownFieldException extends RabbitDMLException {

	public UnKnownFieldException(String message) {
		super(message);
	}

}

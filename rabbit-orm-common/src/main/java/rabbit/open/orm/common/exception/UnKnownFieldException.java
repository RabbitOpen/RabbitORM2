package rabbit.open.orm.common.exception;

@SuppressWarnings("serial")
public class UnKnownFieldException extends RabbitDMLException {

	public UnKnownFieldException(String message) {
		super(message);
	}

}

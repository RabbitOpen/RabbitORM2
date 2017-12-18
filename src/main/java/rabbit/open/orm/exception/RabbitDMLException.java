package rabbit.open.orm.exception;

@SuppressWarnings("serial")
public class RabbitDMLException extends RuntimeException{

	public RabbitDMLException(String message, Throwable cause) {
		super(message, cause);
	}

	public RabbitDMLException(String message) {
		super(message);
	}

	public RabbitDMLException(Throwable cause) {
		super(cause);
	}
	
}

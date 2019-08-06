package rabbit.open.common.exception;

@SuppressWarnings("serial")
public class RabbitDDLException extends RuntimeException {

	public RabbitDDLException(Throwable cause) {
		super(cause);
	}

	public RabbitDDLException(String msg) {
		super(msg);
	}
}

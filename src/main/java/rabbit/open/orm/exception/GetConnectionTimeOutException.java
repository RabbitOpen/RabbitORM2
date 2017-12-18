package rabbit.open.orm.exception;

@SuppressWarnings("serial")
public class GetConnectionTimeOutException extends RabbitORMException{

	public GetConnectionTimeOutException(String message) {
		super(message);
	}
	
}

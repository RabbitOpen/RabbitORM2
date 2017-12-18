package rabbit.open.orm.exception;

@SuppressWarnings("serial")
public class DataSourceClosedException extends RabbitORMException{

	public DataSourceClosedException(String message) {
		super(message);
	}
	
}

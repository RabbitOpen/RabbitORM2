package rabbit.open.common.exception;

@SuppressWarnings("serial")
public class DataSourceClosedException extends RabbitORMException {

	public DataSourceClosedException(String message) {
		super(message);
	}

}

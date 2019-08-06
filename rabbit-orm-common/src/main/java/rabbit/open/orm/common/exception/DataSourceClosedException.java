package rabbit.open.orm.common.exception;

@SuppressWarnings("serial")
public class DataSourceClosedException extends RabbitORMException {

	public DataSourceClosedException(String message) {
		super(message);
	}

}

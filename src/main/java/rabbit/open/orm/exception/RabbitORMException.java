package rabbit.open.orm.exception;

import java.sql.SQLException;

/**
 * <b>Description: 	ORM异常基类</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@SuppressWarnings("serial")
public class RabbitORMException extends SQLException{

	public RabbitORMException() {
		super();
	}
	public RabbitORMException(String message, Throwable cause) {
		super(message, cause);
	}

	public RabbitORMException(String message) {
		super(message);
	}

	public RabbitORMException(Throwable cause) {
		super(cause);
	}
}

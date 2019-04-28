package rabbit.open.orm.exception;

import java.sql.Connection;

/**
 * <b>Description session 持有超时</b>.
 */
@SuppressWarnings("serial")
public class SessionHoldOverTimeException extends RabbitDMLException {

	public SessionHoldOverTimeException(Connection session) {
		super("session[" + session + "] is holded for a long time");
	}

}

package rabbit.open.orm.common.exception;

import java.sql.Connection;

/**
 * <b>Description session 持有超时</b>.
 */
@SuppressWarnings("serial")
public class SessionHoldOverTimeException extends RabbitDMLException {

	public SessionHoldOverTimeException(Connection session, long seconds) {
		super("session[" + session + "] is hold over " + seconds + " seconds!");
	}

}

package rabbit.open.orm.pool;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.springframework.jdbc.datasource.ConnectionHolder;

import rabbit.open.orm.pool.jpa.Session;

/**
 * <b>@description 没有连接对象的ConnectionHolder </b>
 */
public class RabbitConnectionHolder extends ConnectionHolder {

	private Logger logger = Logger.getLogger(getClass());

	public RabbitConnectionHolder() {
		super(new EmptyConnection());
		setConnection(null);
	}

	@Override
	public boolean hasConnection() {
		return super.hasConnection();
	}

	@Override
	public void setConnection(Connection connection) {
		super.setConnection(connection);
		if (null == connection) {
			return;
		}
		try {
			if (SessionFactory.isTransactionOpen()
					&& connection.getAutoCommit()) {
				connection.setAutoCommit(false);
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * <b>@description 什么都不做的数据库连接对象 </b>
	 */
	private static class EmptyConnection extends Session {

	}

}

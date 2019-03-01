package rabbit.open.orm.pool;

import java.sql.Connection;

import org.springframework.jdbc.datasource.ConnectionHolder;

import rabbit.open.orm.pool.jpa.Session;

/**
 * <b>@description 没有连接对象的ConnectionHolder </b>
 */
public class RabbitConnectionHolder extends ConnectionHolder {

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
		if (SessionFactory.isTransactionOpen()) {
			SessionFactory.disableAutoCommit(connection);
		}
	}

	/**
	 * <b>@description 什么都不做的数据库连接对象 </b>
	 */
	private static class EmptyConnection extends Session {

	}

}

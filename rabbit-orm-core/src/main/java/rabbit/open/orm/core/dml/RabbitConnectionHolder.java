package rabbit.open.orm.core.dml;

import java.sql.Connection;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.datasource.Session;

/**
 * <b>@description 没有连接对象的ConnectionHolder </b>
 */
public class RabbitConnectionHolder extends ConnectionHolder {

	private SessionFactory factory;
	
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
			Map<Object, Object> resourceMap = TransactionSynchronizationManager.getResourceMap();
			for (Entry<Object, Object> entry : resourceMap.entrySet()) {
				if (entry.getValue() == this && entry.getKey() instanceof DataSource) {
					factory.setTransactionIsolation(connection, (DataSource) entry.getKey());
					break;
				}
			}
			SessionFactory.disableAutoCommit(connection);
		}
	}

	public void setFactory(SessionFactory factory) {
		this.factory = factory;
	}
	
	/**
	 * <b>@description 什么都不做的数据库连接对象 </b>
	 */
	private static class EmptyConnection extends Session {

	}

}

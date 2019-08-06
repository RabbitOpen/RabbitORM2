package rabbit.open.orm.core.spring;

import java.sql.Connection;
import java.sql.Savepoint;

import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionDefinition;

public class TransactionObject implements SavepointManager {
	
	private Savepoint savePoint;
	
	private Connection connection;
	
	private int propagation = TransactionDefinition.PROPAGATION_REQUIRED;
	
	private int transactionIsolationLevel = -1;

	@Override
	public Object createSavepoint() {
		return null;
	}

	@Override
	public void rollbackToSavepoint(Object obj) {
		// do nothing
	}

	@Override
	public void releaseSavepoint(Object obj) {
		// do nothing
	}
	
	public void setSavePoint(Savepoint savePoint) {
		this.savePoint = savePoint;
	}
	
	public Savepoint getSavePoint() {
		return savePoint;
	}
	
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public int getPropagation() {
		return propagation;
	}
	
	public void setPropagation(int propagation) {
		this.propagation = propagation;
	}

	public int getTransactionIsolationLevel() {
		return transactionIsolationLevel;
	}

	public void setTransactionIsolationLevel(int transactionIsolationLevel) {
		this.transactionIsolationLevel = transactionIsolationLevel;
	}
	
}

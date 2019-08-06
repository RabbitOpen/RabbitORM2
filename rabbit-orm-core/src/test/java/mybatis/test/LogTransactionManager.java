package mybatis.test;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

import rabbit.open.orm.pool.SessionFactory;
import rabbit.open.orm.spring.RabbitTransactionManager;

@SuppressWarnings("serial")
public class LogTransactionManager extends RabbitTransactionManager {

	private Logger logger = Logger.getLogger(getClass());

	public LogTransactionManager() {
		super();
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		super.setSessionFactory(sessionFactory);
	}

	@Override
	public Object getResourceFactory() {
		return super.getResourceFactory();
	}

	@Override
	protected Object doGetTransaction() {
		logger.info("doGetTransaction");
		return super.doGetTransaction();
	}

	@Override
	protected void doBegin(Object obj, TransactionDefinition def) {
		if (def.isReadOnly()) {
			return;
		}
		logger.info("doBegin: " + obj);
		super.doBegin(obj, def);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		if (status.isReadOnly()) {
			return;
		}
		logger.info("doCommit: " + status.getTransaction());
		super.doCommit(status);
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {

	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		if (status.isReadOnly()) {
			return;
		}
		logger.info("doRollback: " + status.getTransaction());
		super.doRollback(status);
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		boolean existingTransaction = super.isExistingTransaction(transaction);
		logger.info("isExistingTransaction: " + existingTransaction);
		return existingTransaction;
	}

	@Override
	protected Object doSuspend(Object transaction) {
		logger.info("doSuspend: " + transaction);
		return null;
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		logger.info("doResume: " + transaction + ", " + suspendedResources);
	}
}

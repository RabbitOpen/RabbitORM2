package rabbit.open.orm.core.spring;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;

import rabbit.open.orm.core.dml.SessionFactory;

/**
 * <b>@description 事务管理器 </b>
 */
@SuppressWarnings("serial")
public class RabbitTransactionManager extends
        AbstractPlatformTransactionManager implements
        ResourceTransactionManager {

    private transient SessionFactory sessionFactory;

    public RabbitTransactionManager() {
        setNestedTransactionAllowed(true);
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Object getResourceFactory() {
        return sessionFactory;
    }

    @Override
    protected Object doGetTransaction() {
        return new TransactionObject();
    }

    @Override
    protected void doBegin(Object obj, TransactionDefinition def) {
        if (def.isReadOnly()) {
            return;
        }
        TransactionObject tObj = (TransactionObject) obj;
        tObj.setPropagation(def.getPropagationBehavior());
        tObj.setTransactionIsolationLevel(def.getIsolationLevel());
        SessionFactory.beginTransaction(obj, sessionFactory);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        if (status.isReadOnly()) {
            return;
        }
        SessionFactory.commit(status.getTransaction(), sessionFactory);
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
    	
    }
    
    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        if (status.isReadOnly()) {
            return;
        }
        SessionFactory.rollBack(status.getTransaction(), sessionFactory);
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
    	return false;
    }
    
    @Override
    protected Object doSuspend(Object transaction) {
    	return null;
    }
    
    @Override
    protected void doResume(Object transaction, Object suspendedResources) {
    	
    }
    
}

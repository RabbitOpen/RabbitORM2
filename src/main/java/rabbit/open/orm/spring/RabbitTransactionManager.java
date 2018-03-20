package rabbit.open.orm.spring;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;

import rabbit.open.orm.pool.SessionFactory;

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
        return new Object();
    }

    @Override
    protected void doBegin(Object obj, TransactionDefinition def) {
        if (def.isReadOnly()) {
            return;
        }
        SessionFactory.beginTransaction(obj);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        if (status.isReadOnly()) {
            return;
        }
        SessionFactory.commit(status.getTransaction());
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        if (status.isReadOnly()) {
            return;
        }
        SessionFactory.rollBack(status.getTransaction());
    }

}

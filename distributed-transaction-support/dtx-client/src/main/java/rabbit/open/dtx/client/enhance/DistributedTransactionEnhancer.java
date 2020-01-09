package rabbit.open.dtx.client.enhance;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.annotation.DistributedTransaction;
import rabbit.open.dtx.common.annotation.Propagation;
import rabbit.open.dtx.common.context.DistributedTransactionContext;
import rabbit.open.dtx.common.exception.DistributedTransactionException;
import rabbit.open.dtx.common.exception.DtxException;
import rabbit.open.dtx.common.nio.client.DistributedTransactionManager;
import rabbit.open.dtx.common.nio.client.DistributedTransactionObject;
import rabbit.open.dtx.common.spring.enhance.AbstractAnnotationEnhancer;
import rabbit.open.dtx.common.spring.enhance.PointCutHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * distributed transaction 增强器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@SuppressWarnings("serial")
public class DistributedTransactionEnhancer extends AbstractAnnotationEnhancer<DistributedTransaction> {

    private transient Logger log = LoggerFactory.getLogger(getClass());

    // 异步处理的核心线程数
    protected int core = 5;

    // 异步处理的最大线程数
    protected int maxConcurrence = 20;

    protected transient ThreadPoolExecutor tpe;

    // 事务管理器
    protected DistributedTransactionManager transactionManger;

    @Override
    protected PointCutHandler<DistributedTransaction> getHandler() {
        return (invocation, annotation) -> {
            if (Propagation.NESTED == annotation.propagation() && !transactionManger.isTransactionOpen(invocation.getMethod())) {
                log.debug("execute {} without transaction", invocation.getMethod().getName());
                try {
                    return invocation.proceed();
                } catch (Throwable e) {
                    throw new DistributedTransactionException(e);
                }
            }
            if (annotation.transactionTimeoutSeconds() == Long.MAX_VALUE) {
                return syncProcess(invocation, annotation);
            } else {
                return asyncProcess(invocation, annotation);
            }
        };
    }

    /***
     * 异步处理
     * @param    invocation
     * @param    annotation
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    private Object asyncProcess(MethodInvocation invocation, DistributedTransaction annotation) {
        log.debug("{} beginTransaction", invocation.getMethod().getName());
        transactionManger.beginTransaction(invocation.getMethod());
        log.debug("{} begin to execute business", invocation.getMethod().getName());
        DistributedTransactionObject currentTransactionObject = transactionManger.getCurrentTransactionObject();
        Future<Object> future = getExecutor().submit(() -> {
            try {
                DistributedTransactionContext.setDistributedTransactionObject(currentTransactionObject);
                return invocation.proceed();
            } catch (Throwable e) {
                if (getRoot(e) instanceof DtxException) {
                    throw (DtxException) getRoot(e);
                }
                throw new DistributedTransactionException(e);
            } finally {
                DistributedTransactionContext.clear();
            }
        });
        try {
            Object result = future.get(annotation.transactionTimeoutSeconds(), TimeUnit.SECONDS);
            doCommit(invocation);
            return result;
        } catch (Exception e) {
            doRollback(invocation, annotation);
            if (getRoot(e) instanceof DtxException) {
                throw (DtxException) getRoot(e);
            }
            throw new DistributedTransactionException(e);
        }
    }

    private void doCommit(MethodInvocation invocation) {
        log.debug("{} begin to commit ", invocation.getMethod().getName());
        transactionManger.commit(invocation.getMethod());
        log.debug("{} commit success ", invocation.getMethod().getName());
    }

    /**
     * 同步处理
     * @param invocation
     * @param annotation
     * @author xiaoqianbin
     * @date 2019/12/4
     **/
    private Object syncProcess(MethodInvocation invocation, DistributedTransaction annotation) {
        try {
            log.debug("{} beginTransaction", invocation.getMethod().getName());
            transactionManger.beginTransaction(invocation.getMethod());
            log.debug("{} begin to execute business", invocation.getMethod().getName());
            Object result = invocation.proceed();
            doCommit(invocation);
            return result;
        } catch (Throwable e) {
            doRollback(invocation, annotation);
            if (getRoot(e) instanceof DtxException) {
                throw (DtxException) getRoot(e);
            }
            throw new DistributedTransactionException(e);
        }
    }

    private Throwable getRoot(Throwable e) {
        Throwable r = e;
        while (null != r.getCause()) {
            r = r.getCause();
            if (r instanceof DtxException) {
                break;
            }
        }
        return r;
    }

    private void doRollback(MethodInvocation invocation, DistributedTransaction annotation) {
        log.debug("{} begin to rollback ", invocation.getMethod().getName());
        transactionManger.rollback(invocation.getMethod(), annotation.rollbackTimeoutSeconds());
        log.debug("{} rollback end ", invocation.getMethod().getName());
    }

    private ThreadPoolExecutor getExecutor() {
        if (null != tpe) {
            return tpe;
        }
        synchronized (this) {
            if (null != tpe) {
                return tpe;
            } else {
                tpe = new ThreadPoolExecutor(core, maxConcurrence, 5, TimeUnit.MINUTES,
                        new ArrayBlockingQueue<>(1000), (r, executor) -> {
                    throw new DistributedTransactionException("DTX task pool is full!");
                });
                return tpe;
            }
        }
    }

    public void setCore(int core) {
        this.core = core;
    }

    public void setMaxConcurrence(int maxConcurrence) {
        this.maxConcurrence = maxConcurrence;
    }

    public void setTransactionManger(DistributedTransactionManager transactionManger) {
        this.transactionManger = transactionManger;
    }
}

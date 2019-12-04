package rabbit.open.dtx.client.enhance.ext;

import org.aopalliance.intercept.MethodInvocation;
import rabbit.open.dtx.client.context.DistributedTransactionManger;
import rabbit.open.dtx.client.enhance.AbstractAnnotationEnhancer;
import rabbit.open.dtx.client.enhance.PointCutHandler;
import rabbit.open.dtx.client.exception.DistributedTransactionException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * distributed transaction 增强器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class DistributedTransactionEnhancer extends AbstractAnnotationEnhancer<DistributedTransaction> {

    // 异步处理的核心线程数
    protected int core = 5;

    // 异步处理的最大线程数
    protected int maxConcurrence = 20;

    protected transient ThreadPoolExecutor tpe;

    // 事务管理器
    protected DistributedTransactionManger transactionManger;

    @Override
    protected PointCutHandler<DistributedTransaction> getHandler() {
        return (invocation, annotation) -> {
            DistributedTransactionObject transactionObject = transactionManger.newTransactionObject();
            if (annotation.timeoutSeconds() == Long.MAX_VALUE) {
                return syncProcess(invocation, transactionObject);
            } else {
                return asyncProcess(invocation, annotation, transactionObject);
            }
        };
    }

    /***
     * 异步处理
     * @param	invocation
	 * @param	annotation
	 * @param	transactionObject
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    private Object asyncProcess(MethodInvocation invocation, DistributedTransaction annotation, DistributedTransactionObject transactionObject) {
        Future<Object> future = getExecutor().submit(() -> {
            try {
                transactionManger.beginTransaction(transactionObject);
                return invocation.proceed();
            } catch (Throwable e) {
                throw new DistributedTransactionException(e);
            }
        });
        try {
            Object result = future.get(annotation.timeoutSeconds(), TimeUnit.SECONDS);
            transactionManger.commit(transactionObject);
            return result;
        } catch (Exception e) {
            transactionManger.rollback(transactionObject);
            throw new DistributedTransactionException(e);
        }
    }

    /**
     * 同步处理
     * @param	invocation
	 * @param	transactionObject
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    private Object syncProcess(MethodInvocation invocation, DistributedTransactionObject transactionObject) {
        try {
            transactionManger.beginTransaction(transactionObject);
            Object result = invocation.proceed();
            transactionManger.commit(transactionObject);
            return result;
        } catch (Throwable e) {
            transactionManger.rollback(transactionObject);
            throw new DistributedTransactionException(e);
        }
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

    public void setTransactionManger(DistributedTransactionManger transactionManger) {
        this.transactionManger = transactionManger;
    }
}

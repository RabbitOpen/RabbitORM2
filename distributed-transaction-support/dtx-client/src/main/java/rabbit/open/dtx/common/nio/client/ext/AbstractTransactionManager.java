package rabbit.open.dtx.common.nio.client.ext;

import rabbit.open.dtx.common.context.DistributedTransactionContext;
import rabbit.open.dtx.common.nio.client.*;
import rabbit.open.dtx.common.nio.client.annotation.DistributedTransaction;
import rabbit.open.dtx.common.nio.exception.GetConnectionTimeoutException;
import rabbit.open.dtx.common.nio.exception.NetworkException;
import rabbit.open.dtx.common.nio.exception.RpcException;
import rabbit.open.dtx.common.nio.exception.TimeoutException;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.nio.pub.protocol.RpcProtocol;
import rabbit.open.dtx.common.spring.anno.Namespace;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * 抽象事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@SuppressWarnings("serial")
public abstract class AbstractTransactionManager implements DistributedTransactionManager {

    private transient TransactionHandler defaultHandler;

    private transient DtxChannelAgentPool pool;

    @Override
    public final void beginTransaction(Method method) {
        if (isTransactionOpen(method)) {
            return;
        }
        if (null == getCurrentTransactionObject()) {
            DistributedTransactionObject tranObj = newTransactionObject();
            tranObj.setPromoter(true);
            tranObj.setIsolation(method.getAnnotation(DistributedTransaction.class).isolation());
            tranObj.setTransactionOwner(method);
            DistributedTransactionContext.setDistributedTransactionObject(tranObj);
        }
    }

    @Override
    public final void rollback(Method method, long timeoutSeconds) {
        if (isTransactionPromoter(method)) {
            // 只有最外层的发起者才能回滚事务
            try {
                DistributedTransactionContext.setRollbackTimeout(timeoutSeconds);
                doRollback();
            } finally {
                DistributedTransactionContext.clear();
            }
        }
    }

    /**
     * 判断method是否就是分布式事务的原始发起者
     * @param method
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    protected final boolean isTransactionPromoter(Method method) {
        return isBranchPromoter(method) && getCurrentTransactionObject().isPromoter();
    }

    /**
     * 判断method是否就是分支事务的发起者
     * @param method
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    protected final boolean isBranchPromoter(Method method) {
        return isTransactionOpen(method) && getCurrentTransactionObject().getTransactionOwner().equals(method);
    }

    @Override
    public final void commit(Method method) {
        if (isBranchPromoter(method)) {
            try {
                doCommit(method);
            } finally {
                DistributedTransactionContext.clear();
            }
        }
    }

    /**
     * 判断是否已经开启事务
     * @param method
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    @Override
    public boolean isTransactionOpen(Method method) {
        return null != getCurrentTransactionObject();
    }

    @Override
    public final DistributedTransactionObject getCurrentTransactionObject() {
        return DistributedTransactionContext.getDistributedTransactionObject();
    }

    /**
     * 新建事务对象
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    protected DistributedTransactionObject newTransactionObject() {
        return new DistributedTransactionObject(getTransactionGroupId());
    }

    @Override
    public Long getTransactionBranchId() {
        if (null == getCurrentTransactionObject().getTxBranchId()) {
            Long txBranchId = getTransactionHandler().getTransactionBranchId(getCurrentTransactionObject().getTxGroupId(), getApplicationName());
            getCurrentTransactionObject().setTxBranchId(txBranchId);
        }
        return getCurrentTransactionObject().getTxBranchId();
    }

    @Override
    public final Long getTransactionGroupId() {
        return getTransactionHandler().getTransactionGroupId(getApplicationName());
    }

    /**
     * 提交
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    protected void doCommit(Method method) {
        DistributedTransactionObject tranObj = getCurrentTransactionObject();
        if (isTransactionPromoter(method)) {
            getTransactionHandler().doCommit(tranObj.getTxGroupId(), tranObj.getTxBranchId(), getApplicationName());
        } else {
            getTransactionHandler().doBranchCommit(tranObj.getTxGroupId(), tranObj.getTxBranchId(), getApplicationName());
        }
    }

    /**
     * 回滚
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    protected void doRollback() {
        getTransactionHandler().doRollback(getCurrentTransactionObject().getTxGroupId(), getApplicationName());
    }

    /**
     * 获取事务处理器
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    public TransactionHandler getTransactionHandler() {
        return defaultHandler;
    }

    /**
     * rpc调用超时时间
     * @author xiaoqianbin
     * @date 2019/12/11
     **/
    protected abstract long getRpcTimeoutSeconds();

    /**
     * 获取消息监听器
     * @author xiaoqianbin
     * @date 2019/12/10
     **/
    public abstract AbstractMessageListener getMessageListener();

    /**
     * 分布式事务服务端信息
     * @author xiaoqianbin
     * @date 2019/12/10
     **/
    public abstract List<Node> getServerNodes();

    @PostConstruct
    public void init() throws IOException {
        pool = new DtxChannelAgentPool(this);
        InvocationHandler handler = new TransactionClientProxy(pool);
        defaultHandler = (TransactionHandler) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TransactionHandler.class}, handler);
    }

    @PreDestroy
    public void destroy() {
        if (null != pool) {
            pool.gracefullyShutdown();
        }
        if (null != getMessageListener()) {
            getMessageListener().close();
        }
    }

    private class TransactionClientProxy implements InvocationHandler {

        DtxChannelAgentPool pool;

        String namespace;

        public TransactionClientProxy(DtxChannelAgentPool pool) {
            this.pool = pool;
            Namespace annotation = TransactionHandler.class.getAnnotation(Namespace.class);
            namespace = annotation.value();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("equals".equals(method.getName())) {
                return false;
            }
            if ("hashCode".equals(method.getName())) {
                return -1;
            }
            if ("toString".equals(method.getName())) {
                return TransactionHandler.class.getName();
            }
            return doInvoke(method, args);
        }

        private Object doInvoke(Method method, Object[] args) {
            Object data;
            ChannelAgent agent = null;
            try {
                RpcProtocol protocol = new RpcProtocol(namespace, method.getName(), method.getParameterTypes(), args);
                agent = this.pool.getResource();
                FutureResult result = agent.send(protocol);
                agent.release();
                Long timeout = DistributedTransactionContext.getRollbackTimeout();
                data = result.getData(null == timeout ? getRpcTimeoutSeconds() : timeout);
            } catch (TimeoutException | GetConnectionTimeoutException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RpcException(e);
            } catch (NetworkException e) {
                if (null != agent) {
                    agent.destroy();
                }
                // 网络IO异常就直接重试
                return doInvoke(method, args);
            }
            if (data instanceof RpcException) {
                throw (RpcException) data;
            }
            return data;
        }
    }
}
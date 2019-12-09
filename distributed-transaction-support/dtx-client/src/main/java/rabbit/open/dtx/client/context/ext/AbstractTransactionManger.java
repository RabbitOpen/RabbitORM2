package rabbit.open.dtx.client.context.ext;

import rabbit.open.dts.common.rpc.nio.pub.TransactionHandler;
import rabbit.open.dtx.client.context.DistributedTransactionContext;
import rabbit.open.dtx.client.context.DistributedTransactionManger;
import rabbit.open.dtx.client.enhance.DistributedTransactionObject;

import java.lang.reflect.Method;

/**
 * 抽象事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@SuppressWarnings("serial")
public abstract class AbstractTransactionManger implements DistributedTransactionManger {

    @Override
    public final void beginTransaction(Method method) {
        if (isTransactionOpen()) {
            return;
        }
        if (null == getCurrentTransactionObject()) {
            DistributedTransactionObject tranObj = newTransactionObject();
            tranObj.setPromoter(true);
            tranObj.setTransactionOwner(method);
            DistributedTransactionContext.setDistributedTransactionObject(tranObj);
        }
    }

    @Override
    public final void rollback(Method method) {
        if (isTransactionPromoter(method)) {
            // 只有最外层的发起者才能回滚事务
            try {
                doRollback();
            } finally {
                DistributedTransactionContext.clear();
            }
        }
    }

    /**
     * 判断method是否就是分布式事务的原始发起者
     * @param	method
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    protected final boolean isTransactionPromoter(Method method) {
        return isBranchPromoter(method) && getCurrentTransactionObject().isPromoter();
    }

    /**
     * 判断method是否就是分支事务的发起者
     * @param	method
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    protected final boolean isBranchPromoter(Method method) {
        return isTransactionOpen() && getCurrentTransactionObject().getTransactionOwner().equals(method);
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
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    @Override
    public boolean isTransactionOpen() {
        return null != getCurrentTransactionObject();
    }

    @Override
    public final DistributedTransactionObject getCurrentTransactionObject() {
        return DistributedTransactionContext.getDistributedTransactionObject();
    }

    /**
     * 新建事务对象
     * @author  xiaoqianbin
     * @date    2019/12/5
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
        return getTransactionHandler().getTransactionGroupId();
    }

    /**
     * 提交
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    protected void doCommit(Method method) {
        DistributedTransactionObject tranObj = getCurrentTransactionObject();
        if (isTransactionPromoter(method)) {
            getTransactionHandler().doCommit(tranObj.getTxGroupId(), tranObj.getTxBranchId());
        } else {
            getTransactionHandler().doBranchCommit(tranObj.getTxGroupId(), tranObj.getTxBranchId(), getApplicationName());
        }
    }

    /**
     * 回滚
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    protected void doRollback() {
        getTransactionHandler().doRollback(getCurrentTransactionObject().getTxGroupId());
    }

    /**
     * 获取事务处理器
     * @author  xiaoqianbin
     * @date    2019/12/5
     **/
    protected abstract TransactionHandler getTransactionHandler();

}

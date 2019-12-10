package rabbit.open.dtx.common.nio.server.ext;

import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.nio.server.TxStatus;

/**
 * 抽象服务端事务处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public abstract class AbstractServerTransactionHandler implements TransactionHandler {

    @Override
    public void doBranchCommit(Long txGroupId, Long txBranchId, String applicationName) {
        persistBranchInfo(txGroupId, txBranchId, applicationName, TxStatus.COMMIT);
    }

    @Override
    public void doCommit(Long txGroupId, Long txBranchId, String applicationName) {
        doBranchCommit(txGroupId, txBranchId, applicationName);
        persistGroupId(txGroupId, TxStatus.COMMIT);
        doCommitByGroupId(txGroupId);
    }

    @Override
    public void doRollback(Long txGroupId) {
        persistGroupId(txGroupId, TxStatus.ROLLBACK);
        doRollbackByGroupId(txGroupId);
    }

    @Override
    public Long getTransactionBranchId(Long txGroupId, String applicationName) {
        Long txBranchId = getNextGlobalId();
        persistBranchInfo(txGroupId, txBranchId, applicationName, TxStatus.OPEN);
        return txBranchId;
    }

    @Override
    public Long getTransactionGroupId() {
        Long txGroupId = getNextGlobalId();
        persistGroupId(txGroupId, TxStatus.OPEN);
        return txGroupId;
    }

    /**
     * 根据事务组id进行提交
     * @param	txGroupId
     * @author  xiaoqianbin
     * @date    2019/12/10
     **/
    protected abstract void doCommitByGroupId(Long txGroupId);

    /**
     * 根据事务组id进行回滚
     * @param	txGroupId
     * @author  xiaoqianbin
     * @date    2019/12/10
     **/
    protected abstract void doRollbackByGroupId(Long txGroupId);

    /**
     * 生成一个全局唯一的id
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected abstract Long getNextGlobalId();

    /**
     * 持久化分组id信息
     * @param	txGroupId
     * @param	txStatus 事务状态
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected abstract void persistGroupId(Long txGroupId, TxStatus txStatus);

    /**
     * 持久化分支id
     * @param	txGroupId
	 * @param	txBranchId
	 * @param	applicationName
	 * @param	txStatus 事务状态
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected abstract void persistBranchInfo(Long txGroupId, Long txBranchId, String applicationName, TxStatus txStatus);
}

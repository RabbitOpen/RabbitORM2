package rabbit.open.dtx.common.nio.server.ext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.nio.server.TxStatus;

/**
 * 抽象服务端事务处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public abstract class AbstractServerTransactionHandler implements TransactionHandler {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void doBranchCommit(Long txGroupId, Long txBranchId, String applicationName) {
        if (null == txBranchId) {
            return;
        }
        persistBranchInfo(txGroupId, txBranchId, applicationName, TxStatus.COMMIT);
        logger.debug("{} doBranchCommit, txGroupId: {},  txBranchId: {}", applicationName, txGroupId, txBranchId);
    }

    @Override
    public void doCommit(Long txGroupId, Long txBranchId, String applicationName) {
        doBranchCommit(txGroupId, txBranchId, applicationName);
        persistGroupId(txGroupId, TxStatus.COMMIT);
        logger.debug("{} doGroupCommit, txGroupId: {},  txBranchId: {}", applicationName, txGroupId, txBranchId);
        doCommitByGroupId(txGroupId);
    }

    @Override
    public void doRollback(Long txGroupId, String applicationName) {
        persistGroupId(txGroupId, TxStatus.ROLLBACK);
        logger.debug("{} doRollback txGroupId: {}", applicationName, txGroupId);
        doRollbackByGroupId(txGroupId);
    }

    @Override
    public Long getTransactionBranchId(Long txGroupId, String applicationName) {
        Long txBranchId = getNextGlobalId();
        persistBranchInfo(txGroupId, txBranchId, applicationName, TxStatus.OPEN);
        logger.debug("'{} |'txGroupId({}) | getTransactionBranchId | {}", applicationName, txGroupId, txBranchId);
        return txBranchId;
    }

    @Override
    public Long getTransactionGroupId(String applicationName) {
        Long txGroupId = getNextGlobalId();
        persistGroupId(txGroupId, applicationName, TxStatus.OPEN);
        logger.debug("{} open transaction, txGroupId: {}", applicationName, txGroupId);
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
     * @param   applicationName
     * @param	txStatus 事务状态
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected abstract void persistGroupId(Long txGroupId, String applicationName, TxStatus txStatus);

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

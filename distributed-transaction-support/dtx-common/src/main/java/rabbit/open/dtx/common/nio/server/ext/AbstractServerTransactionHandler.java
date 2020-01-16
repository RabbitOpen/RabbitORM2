package rabbit.open.dtx.common.nio.server.ext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.pub.ext.AbstractNetEventHandler;
import rabbit.open.dtx.common.nio.pub.inter.TransactionHandler;
import rabbit.open.dtx.common.nio.server.DtxServer;
import rabbit.open.dtx.common.nio.server.TxStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抽象服务端事务处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public abstract class AbstractServerTransactionHandler implements TransactionHandler {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected Map<Long, RollbackListener> rollbackListenerMap = new ConcurrentHashMap<>();

    private DtxServer dtxServer;

    @Override
    public void doBranchCommit(Long txGroupId, Long txBranchId, String applicationName) {
        if (null == txBranchId) {
            return;
        }
        persistBranchInfo(txGroupId, txBranchId, applicationName, TxStatus.COMMITTED);
        logger.debug("{} doBranchCommit, txGroupId: {}, txBranchId: {}", applicationName, txGroupId, txBranchId);
    }

    @Override
    public void doCommit(Long txGroupId, Long txBranchId, String applicationName) {
        doBranchCommit(txGroupId, txBranchId, applicationName);
        persistGroupStatus(txGroupId, applicationName, TxStatus.COMMITTED);
        logger.debug("{} doGroupCommit, txGroupId: {}, txBranchId: {}", applicationName, txGroupId, txBranchId);
        doCommitByGroupId(txGroupId, applicationName);
    }

    @Override
    public void doRollback(Long txGroupId, String applicationName) {
        persistGroupStatus(txGroupId, applicationName, TxStatus.ROLL_BACKED);
        logger.debug("{} doRollback txGroupId: {}", applicationName, txGroupId);
        rollbackListenerMap.put(txGroupId, new RollbackListener(AbstractNetEventHandler.getCurrentAgent(),
                AbstractServerEventHandler.getCurrentRequestId()));
        doRollbackByGroupId(txGroupId, applicationName);
        AbstractServerEventHandler.suspendRequest();
    }

    protected boolean rollbackCompleted(Long txGroupId) {
        RollbackListener listener = rollbackListenerMap.get(txGroupId);
        if (null != listener) {
            boolean result = listener.rollbackCompleted();
            logger.debug("roll back tx({}), result: {}", txGroupId, result);
            return result;
        }
        return false;
    }

    /**
     * 生成事务分支id
     * @param	txGroupId
	 * @param	applicationName
     * @author  xiaoqianbin
     * @date    2019/12/20
     **/
    @Override
    public Long getTransactionBranchId(Long txGroupId, String applicationName) {
        Long txBranchId = getNextGlobalId();
        logger.debug("[{}] create branch {}-{}", applicationName, txGroupId, txBranchId);
        persistBranchInfo(txGroupId, txBranchId, applicationName, TxStatus.OPEN);
        return txBranchId;
    }

    /**
     * 生成事务组id
     * @param	applicationName
     * @author  xiaoqianbin
     * @date    2019/12/20
     **/
    @Override
    public Long getTransactionGroupId(String applicationName) {
        Long txGroupId = getNextGlobalId();
        openTransaction(txGroupId, applicationName);
        logger.debug("{} open transaction, txGroupId: {}", applicationName, txGroupId);
        return txGroupId;
    }

    /**
     * 根据事务组id进行提交
     * @param	txGroupId
     * @param	applicationName
     * @author  xiaoqianbin
     * @date    2019/12/10
     **/
    protected abstract void doCommitByGroupId(Long txGroupId, String applicationName);

    /**
     * 根据事务组id进行回滚
     * @param	txGroupId
     * @param	applicationName
     * @author  xiaoqianbin
     * @date    2019/12/10
     **/
    protected abstract void doRollbackByGroupId(Long txGroupId, String applicationName);

    /**
     * 生成一个全局唯一的id
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    public abstract Long getNextGlobalId();

    /**
     * 持久化分组id信息
     * @param	txGroupId
     * @param   applicationName
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected abstract void openTransaction(Long txGroupId, String applicationName);

    /**
     * 持久化分组id信息
     * @param	txGroupId
     * @param	applicationName
     * @param	txStatus 事务状态
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected abstract void persistGroupStatus(Long txGroupId, String applicationName, TxStatus txStatus);

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

    public DtxServer getDtxServer() {
        return dtxServer;
    }

    public void setDtxServer(DtxServer dtxServer) {
        this.dtxServer = dtxServer;
    }
}

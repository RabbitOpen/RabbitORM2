package rabbit.open.dtx.common.nio.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import rabbit.open.dtx.common.nio.exception.DistributedTransactionException;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.protocol.CommitMessage;
import rabbit.open.dtx.common.nio.pub.protocol.RollBackMessage;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerTransactionHandler;
import rabbit.open.dtx.common.nio.server.ext.TransactionContext;
import rabbit.open.dtx.common.nio.server.handler.ApplicationDataHandler;

/**
 * 基于内存事务处理器
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class MemoryBasedTransactionHandler extends AbstractServerTransactionHandler {

    // 事务id生成器
    private AtomicLong idGenerator = new AtomicLong(0);

    // 事务信息缓存
    private static final Map<Long, TransactionContext> contextCache = new ConcurrentHashMap<>();

    @Override
    protected void doCommitByGroupId(Long txGroupId, String applicationName) {
        doTransactionByGroupId(txGroupId, applicationName, TxStatus.COMMITTED);
    }

    @Override
    public void confirmBranchCommit(String applicationName, Long txGroupId, Long txBranchId) {
        TransactionContext context = contextCache.get(txGroupId);
        if (null != context) {
            removeBranchTransactionData(txBranchId, context);
            logger.debug("{} confirmBranchCommit [{} --> {}] ", applicationName, txGroupId, txBranchId);
            if (context.getBranchApp().isEmpty()) {
                contextCache.remove(txGroupId);
                logger.debug("tx [{}] commit success", txGroupId);
            }
        }
    }

    /**
     * 获取开启的事务个数(计划放入meta信息)
     * @author  xiaoqianbin
     * @date    2019/12/12
     **/
    public long getOpenedTransactionCount() {
        return contextCache.size();
    }

    // 删除分支事务数据
    private void removeBranchTransactionData(Long txBranchId, TransactionContext context) {
        context.getBranchApp().remove(txBranchId);
        context.getBranchStatus().remove(txBranchId);
    }

    @Override
    public void confirmBranchRollback(String applicationName, Long txGroupId, Long txBranchId) {
        TransactionContext context = contextCache.get(txGroupId);
        if (null != context) {
            removeBranchTransactionData(txBranchId, context);
            logger.debug("{} confirmBranchRollback [{} --> {}] ", applicationName, txGroupId, txBranchId);
            if (context.getBranchApp().isEmpty()) {
                contextCache.remove(txGroupId);
                boolean result = rollbackCompleted(txGroupId);
                logger.debug("tx [{}] rollback completed, rollback result: {}", txGroupId, result);
            }
        }
    }

    private void doTransactionByGroupId(Long txGroupId, String applicationName, TxStatus txStatus) {
        TransactionContext context = contextCache.get(txGroupId);
        if (null != context) {
            if (!context.getApplicationName().equals(applicationName)) {
                String errMsg = String.format("txGroupId(%s) is created by '%s', but %s by '%s'", txGroupId,
                        context.getApplicationName(), txStatus, applicationName);
                logger.error(errMsg);
                throw new DistributedTransactionException(errMsg);
            }
            if (txStatus == TxStatus.ROLL_BACKED && context.getBranchApp().isEmpty()) {
                contextCache.remove(txGroupId);
                boolean result = rollbackCompleted(txGroupId);
                logger.debug("tx [{}] rollback completed, rollback result: {}", txGroupId, result);
            } else {
                context.setTxStatus(TxStatus.COMMITTING);
                for (Map.Entry<Long, String> entry : context.getBranchApp().entrySet()) {
                    if (TxStatus.COMMITTED == context.getBranchStatus().get(entry.getKey())) {
                        doBranchTransaction(txGroupId, entry, txStatus);
                    }
                }
            }
        }
    }

    // 回滚或者提交分支事务
    private void doBranchTransaction(Long txGroupId, Map.Entry<Long, String> entry, TxStatus txStatus) {
        Long txBranchId = entry.getKey();
        String app = entry.getValue();
        List<ChannelAgent> agents = ApplicationDataHandler.getAgents(app);
        for (ChannelAgent agent : agents) {
            if (!agent.isClosed()) {
                agent.response(TxStatus.COMMITTED == txStatus ? new CommitMessage(app, txGroupId, txBranchId)
                        : new RollBackMessage(app, txGroupId, txBranchId), null);
                logger.debug("delivery {} branch ({} --> {}) ", txStatus, txGroupId, txBranchId);
                break;
            }
        }
    }

    @Override
    protected void doRollbackByGroupId(Long txGroupId, String applicationName) {
        doTransactionByGroupId(txGroupId, applicationName, TxStatus.ROLL_BACKED);
    }

    @Override
    protected Long getNextGlobalId() {
        return idGenerator.getAndAdd(1L);
    }

    @Override
    protected void persistGroupId(Long txGroupId, String applicationName, TxStatus txStatus) {
        if (TxStatus.OPEN == txStatus) {
            TransactionContext context = new TransactionContext(txStatus);
            context.setApplicationName(applicationName);
            contextCache.put(txGroupId, context);
        } else {
            TransactionContext context = contextCache.get(txGroupId);
            if (null != context) {
                context.setTxStatus(txStatus);
            }
        }
    }

    @Override
    protected void persistGroupId(Long txGroupId, TxStatus txStatus) {
        TransactionContext context = contextCache.get(txGroupId);
        if (null != context) {
            context.setTxStatus(txStatus);
        }
    }

    @Override
    protected void persistBranchInfo(Long txGroupId, Long txBranchId, String applicationName, TxStatus txStatus) {
        TransactionContext context = contextCache.get(txGroupId);
        if (null != context) {
            context.persistBranchInfo(txBranchId, applicationName, txStatus);
        }
    }
}

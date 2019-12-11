package rabbit.open.dtx.common.nio.server;

import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.protocol.CommitMessage;
import rabbit.open.dtx.common.nio.pub.protocol.RollBackMessage;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerTransactionHandler;
import rabbit.open.dtx.common.nio.server.ext.TransactionContext;
import rabbit.open.dtx.common.nio.server.handler.ApplicationDataHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    protected void doCommitByGroupId(Long txGroupId) {
        doTransactionByGroupId(txGroupId, TxStatus.COMMIT);
    }

    @Override
    public void confirmBranchCommit(Long txGroupId, Long txBranchId) {
        TransactionContext context = contextCache.get(txGroupId);
        if (null != context) {
            removeBranchTransactionData(txBranchId, context);
            logger.info("confirmBranchCommit [{} - {}] ", txGroupId, txBranchId);
            if (context.getBranchApp().isEmpty()) {
                contextCache.remove(txGroupId);
                logger.info("tx [{}] commit success", txGroupId);
            }
        }
    }

    // 删除分支事务数据
    private void removeBranchTransactionData(Long txBranchId, TransactionContext context) {
        context.getBranchApp().remove(txBranchId);
        context.getBranchStatus().remove(txBranchId);
    }

    @Override
    public void confirmBranchRollback(Long txGroupId, Long txBranchId) {
        TransactionContext context = contextCache.get(txGroupId);
        if (null != context) {
            removeBranchTransactionData(txBranchId, context);
            logger.info("confirmBranchRollback [{} - {}] ", txGroupId, txBranchId);
            if (context.getBranchApp().isEmpty()) {
                contextCache.remove(txGroupId);
                logger.info("tx [{}] rollback success", txGroupId);
            }
        }
    }

    private void doTransactionByGroupId(Long txGroupId, TxStatus txStatus) {
        TransactionContext context = contextCache.get(txGroupId);
        if (null != context) {
            for (Map.Entry<Long, String> entry : context.getBranchApp().entrySet()) {
                doBranchTransaction(txGroupId, entry, txStatus);
            }
        }
    }

    // 分支事务
    private void doBranchTransaction(Long txGroupId, Map.Entry<Long, String> entry, TxStatus txStatus) {
        Long txBranchId = entry.getKey();
        String app = entry.getValue();
        List<ChannelAgent> agents = ApplicationDataHandler.getAgents(app);
        for (ChannelAgent agent : agents) {
            if (!agent.isClosed()) {
                agent.response(TxStatus.COMMIT == txStatus ? new CommitMessage(app, txGroupId, txBranchId)
                        : new RollBackMessage(app, txGroupId, txBranchId), null);
                logger.info("delivery {} branch ({} --> {}) ", txStatus, txGroupId, txBranchId);
                break;
            }
        }
    }

    @Override
    protected void doRollbackByGroupId(Long txGroupId) {
        doTransactionByGroupId(txGroupId, TxStatus.ROLLBACK);
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

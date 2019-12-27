package rabbit.open.dtx.common.nio.server;

import rabbit.open.dtx.common.nio.exception.DistributedTransactionException;
import rabbit.open.dtx.common.nio.pub.CallHelper;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.ext.AbstractNetEventHandler;
import rabbit.open.dtx.common.nio.pub.protocol.CommitMessage;
import rabbit.open.dtx.common.nio.pub.protocol.RollBackMessage;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerTransactionHandler;
import rabbit.open.dtx.common.nio.server.ext.TransactionContext;
import rabbit.open.dtx.common.nio.server.handler.ApplicationDataHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存事务处理器
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class MemoryTransactionHandler extends AbstractServerTransactionHandler {

    // 事务id生成器
    private AtomicLong idGenerator = new AtomicLong(0);

    // 事务信息缓存
    private final Map<Long, TransactionContext> contextCache = new ConcurrentHashMap<>();

    // 锁等待队列
    private final Map<String, LinkedBlockingQueue<LockContext>> lockQueueMap = new ConcurrentHashMap<>();

    @Override
    protected void doCommitByGroupId(Long txGroupId, String applicationName) {
        releaseHoldLocks(txGroupId);
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

    /**
     * 锁数据
     * @param	applicationName
	 * @param	txGroupId
	 * @param	txBranchId
	 * @param	locks
     * @author  xiaoqianbin
     * @date    2019/12/23
     **/
    @Override
    public void lockData(String applicationName, Long txGroupId, Long txBranchId, List<String> locks) {
        for (String l : locks) {
            String lock = new StringBuilder(applicationName).append(l).toString();
            try {
                ReentrantLockPool.lock(applicationName, l);
                TransactionContext context = contextCache.get(txGroupId);
                if (null == context || context.repeatedLockRequest(lock, txBranchId)) {
                    continue;
                }
                if (!lockQueueMap.containsKey(lock)) {
                    // 如果没有人持有该锁，就尝试持有锁
                    if (context.try2HoldLock(lock, txBranchId)) {
                        logger.debug("{} transaction[{}-{}] hold lock {}", applicationName, txGroupId, txBranchId, lock);
                        lockQueueMap.put(lock, new LinkedBlockingQueue<>());
                    }
                } else {
                    logger.debug("{} transaction[{}-{}] is waiting lock {}", applicationName, txGroupId, txBranchId, lock);
                    addAgentToWaitingQueue(lock, txBranchId, context);
                }
            } finally {
                ReentrantLockPool.unlock(applicationName, l);
            }
        }
    }

    /**
     * 释放所有的锁
     * @param	txGroupId
     * @author  xiaoqianbin
     * @date    2019/12/23
     **/
    private void releaseHoldLocks(Long txGroupId) {
        TransactionContext context = contextCache.get(txGroupId);
        if (null == context) {
            return;
        }
        context.releaseHoldLocks(lockQueueMap);
    }

    /**
     * 挂起当前获取锁请求，将请求添加入等待队列
     * @param	lock
	 * @param	txBranchId
	 * @param	context
     * @author  xiaoqianbin
     * @date    2019/12/23
     **/
    private void addAgentToWaitingQueue(String lock, Long txBranchId, TransactionContext context) {
        AbstractServerEventHandler.suspendRequest();
        // 添加锁等待队列
        CallHelper.ignoreExceptionCall(() -> lockQueueMap.get(lock).put(new LockContext(context,
                AbstractServerEventHandler.getCurrentRequestId(),
                AbstractNetEventHandler.getCurrentAgent(), txBranchId)));
        // 添加等待的锁
        context.addWaitingLock(lock, txBranchId);
    }

    // 回滚或者提交分支事务
    private void doBranchTransaction(Long txGroupId, Map.Entry<Long, String> entry, TxStatus txStatus) {
        Long txBranchId = entry.getKey();
        String app = entry.getValue();
        List<ChannelAgent> agents = ApplicationDataHandler.getAgents(app);
        for (ChannelAgent agent : agents) {
            if (!agent.isClosed()) {
                try {
                    agent.notify(TxStatus.COMMITTED == txStatus ? new CommitMessage(app, txGroupId, txBranchId)
                            : new RollBackMessage(app, txGroupId, txBranchId));
                    logger.debug("delivery {} branch ({} --> {}) ", txStatus, txGroupId, txBranchId);
                    break;
                } catch (Exception e) {
                    // TO DO: 忽略节点挂了的异常
                }
            }
        }
        // 回滚/提交 失败就等下次节点上来了继续回滚/提交， 放到redis版做
    }

    @Override
    protected void doRollbackByGroupId(Long txGroupId, String applicationName) {
        releaseHoldLocks(txGroupId);
        doTransactionByGroupId(txGroupId, applicationName, TxStatus.ROLL_BACKED);
    }

    @Override
    protected Long getNextGlobalId() {
        return idGenerator.getAndAdd(1L);
    }

    @Override
    protected void persistGroupId(Long txGroupId, String applicationName, TxStatus txStatus) {
        if (TxStatus.OPEN == txStatus) {
            TransactionContext context = new TransactionContext(txStatus, txGroupId);
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

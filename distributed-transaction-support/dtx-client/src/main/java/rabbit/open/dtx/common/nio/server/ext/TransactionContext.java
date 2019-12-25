package rabbit.open.dtx.common.nio.server.ext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.server.LockContext;
import rabbit.open.dtx.common.nio.server.ReentrantLockPool;
import rabbit.open.dtx.common.nio.server.TxStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 事务上下文信息
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
public class TransactionContext {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private TxStatus txStatus;

    private Long txGroupId;

    // 分支事务状态
    private Map<Long, TxStatus> branchStatus;

    // 分支应用缓存
    private Map<Long, String> branchApp;

    // 持有锁和分支映射关系
    private Map<String, Long> holdLocks;

    // 等待分支和锁映射关系, key是分支， value是锁
    private Map<Long, LinkedBlockingQueue<String>> waitingLocks;

    // 开启事务的应用
    private String applicationName;

    private boolean discard = false;

    private ReentrantLock lock = new ReentrantLock();

    public TransactionContext(TxStatus txStatus, Long txGroupId) {
        this.txStatus = txStatus;
        this.txGroupId = txGroupId;
        branchStatus = new ConcurrentHashMap<>();
        branchApp = new ConcurrentHashMap<>();
        holdLocks = new ConcurrentHashMap<>();
        waitingLocks = new ConcurrentHashMap<>();
    }

    public void setTxStatus(TxStatus txStatus) {
        this.txStatus = txStatus;
    }

    public void persistBranchInfo(Long txBranchId, String applicationName, TxStatus txStatus) {
        branchStatus.put(txBranchId, txStatus);
        branchApp.put(txBranchId, applicationName);
    }

    public Map<Long, TxStatus> getBranchStatus() {
        return branchStatus;
    }

    public Map<Long, String> getBranchApp() {
        return branchApp;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public TxStatus getTxStatus() {
        return txStatus;
    }

    /**
     * 尝试持有该锁
     * @param    lockId
     * @param    branchId
     * @author xiaoqianbin
     * @date 2019/12/24
     **/
    public boolean try2HoldLock(String lockId, Long branchId) {
        try {
            if (lock.tryLock() && !discard) {
                // 如果该context是有效的，就正常持有锁
                callUnconcernedException(() -> holdLocks.put(lockId, branchId));
                return true;
            } else {
                // 该context已经废弃了，无须再持有该锁
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 判断事务分支是否已经持有该锁
     * @param	lockId
	 * @param	branchId
     * @author  xiaoqianbin
     * @date    2019/12/25
     **/
    public boolean repeatedLockRequest(String lockId, Long branchId) {
        return holdLocks.containsKey(lockId) && branchId.equals(holdLocks.get(lockId));
    }

    // 标记等待锁的数据
    public void addWaitingLock(String lockId, Long txBranchId) {
        callUnconcernedException(() -> {
            if (!waitingLocks.containsKey(txBranchId)) {
                waitingLocks.put(txBranchId, new LinkedBlockingQueue<>());
            }
            waitingLocks.get(txBranchId).put(lockId);
        });
    }

    /**
     * 尝试唤醒等待锁的客户端
     * @param    lockId
     * @param    txBranchId
     * @param    requestId
     * @param    agent
     * @author xiaoqianbin
     * @date 2019/12/24
     **/
    public boolean tryWakeupClient(String lockId, Long txBranchId, Long requestId, ChannelAgent agent) {
        if (try2HoldLock(lockId, txBranchId)) {
            if (try2HoldLock(lockId, txBranchId) && waitingLocks.get(txBranchId).remove(lockId)) {
                logger.debug("{} transaction[{}-{}] is waked up, hold lock {}", applicationName, txGroupId, txBranchId, lockId);
            }
            if (waitingLocks.get(txBranchId).isEmpty()) {
                // 如果获取到所有的锁资源就通知客户端, 此时如果通知失败就只有等待锁超时了
                callUnconcernedException(() -> agent.ack(requestId));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 不关心异常的调用，除非你确认这个异常不会发生。或者发生了也不用处理才调用这个方法
     * @param    r
     * @author xiaoqianbin
     * @date 2019/12/23
     **/
    public static void callUnconcernedException(Callback r) {
        try {
            r.execute();
        } catch (Exception e) {
            // to do ignore
        }
    }

    public interface Callback {
        void execute() throws InterruptedException;
    }

    /**
     * 释放持有所有锁
     * @param    lockQueueMap
     * @author xiaoqianbin
     * @date 2019/12/24
     **/
    public void releaseHoldLocks(Map<String, LinkedBlockingQueue<LockContext>> lockQueueMap) {
        try {
            lock.lock();
            discard = true;
            for (Map.Entry<String, Long> entry : holdLocks.entrySet()) {
                wakeUpWaitingQueue(lockQueueMap, entry.getKey());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 尝试从等待队列中唤醒一个等待的客户端
     * @param    lockQueueMap
     * @param    lockId
     * @author xiaoqianbin
     * @date 2019/12/24
     **/
    private void wakeUpWaitingQueue(Map<String, LinkedBlockingQueue<LockContext>> lockQueueMap, String lockId) {
        try {
            ReentrantLockPool.lock(applicationName, lockId.substring(applicationName.length()));
            if (!lockQueueMap.containsKey(lockId)) {
                return;
            }
            LinkedBlockingQueue<LockContext> queue = lockQueueMap.get(lockId);
            while (!queue.isEmpty()) {
                LockContext lockContext = queue.poll();
                if (lockContext.tryWakeup(lockId)) {
                    break;
                }
            }
            if (queue.isEmpty()) {
                lockQueueMap.remove(lockId);
            }
        } finally {
            ReentrantLockPool.unlock(applicationName, lockId.substring(applicationName.length()));
        }
    }

}

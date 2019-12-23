package rabbit.open.dtx.common.nio.server.ext;

import rabbit.open.dtx.common.nio.server.TxStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
public class TransactionContext {

    private TxStatus txStatus;

    // 分支事务状态
    private Map<Long, TxStatus> branchStatus;

    // 分支应用缓存
    private Map<Long, String> branchApp;

    // 持有锁队列
    private LinkedBlockingQueue<String> lockIdQueue;

    // 等待锁队列
    private LinkedBlockingQueue<String> lockIdWaitingQueue;

    // 开启事务的应用
    private String applicationName;

    public TransactionContext(TxStatus txStatus) {
        this.txStatus = txStatus;
        branchStatus = new ConcurrentHashMap<>();
        branchApp = new ConcurrentHashMap<>();
        lockIdQueue = new LinkedBlockingQueue<>();
        lockIdWaitingQueue = new LinkedBlockingQueue<>();
    }

    public TxStatus getTxStatus() {
        return txStatus;
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

    // 标记已经锁的数据
    public void addHoldLock(String id) {
        callUnconcernedException(() -> lockIdQueue.put(id));
    }

    // 标记等待锁的数据
    public void addWaitingLock(String id) {
        callUnconcernedException(() -> lockIdWaitingQueue.put(id));
    }

    /**
     * 不关心异常的调用，除非你确认这个异常不会发生。或者发生了也不用处理才调用这个方法
     * @param	r
     * @author  xiaoqianbin
     * @date    2019/12/23
     **/
    public static void callUnconcernedException(Callback r){
        try {
            r.execute();
        } catch (Exception e) {
            // to do ignore
        }
    }

    public interface Callback {
        void execute() throws Exception;
    }

    public LinkedBlockingQueue<String> getLockIdWaitingQueue() {
        return lockIdWaitingQueue;
    }

    public LinkedBlockingQueue<String> getLockIdQueue() {
        return lockIdQueue;
    }
}

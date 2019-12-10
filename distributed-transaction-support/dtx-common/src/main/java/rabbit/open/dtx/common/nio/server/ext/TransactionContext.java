package rabbit.open.dtx.common.nio.server.ext;

import rabbit.open.dtx.common.nio.server.TxStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public TransactionContext(TxStatus txStatus) {
        this.txStatus = txStatus;
        branchStatus = new ConcurrentHashMap<>();
        branchApp = new ConcurrentHashMap<>();
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
}

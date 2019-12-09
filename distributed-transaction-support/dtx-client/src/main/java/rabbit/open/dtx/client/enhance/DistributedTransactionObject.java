package rabbit.open.dtx.client.enhance;

import java.lang.reflect.Method;

/**
 * 事务对象
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class DistributedTransactionObject {

    // 事务组id
    private Long txGroupId;

    // 事务分支id
    private Long txBranchId;

    // 事务开启方法
    private Method transactionOwner;

    // 是任务的发起人
    private boolean promoter;

    public DistributedTransactionObject(Long txGroupId) {
        setTxGroupId(txGroupId);
    }

    public Long getTxGroupId() {
        return txGroupId;
    }

    public void setTxGroupId(Long txGroupId) {
        this.txGroupId = txGroupId;
    }

    public Method getTransactionOwner() {
        return transactionOwner;
    }

    public void setTransactionOwner(Method transactionOwner) {
        this.transactionOwner = transactionOwner;
    }

    public Long getTxBranchId() {
        return txBranchId;
    }

    public void setTxBranchId(Long txBranchId) {
        this.txBranchId = txBranchId;
    }

    public boolean isPromoter() {
        return promoter;
    }

    public void setPromoter(boolean promoter) {
        this.promoter = promoter;
    }
}

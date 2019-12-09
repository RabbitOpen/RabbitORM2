package rabbit.open.dtx.common.spring;

import rabbit.open.dtx.common.nio.pub.TransactionHandler;

/**
 * 基于内存的事务处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class MemoryTransactionHandler implements TransactionHandler {

    @Override
    public void doBranchCommit(Long txGroupId, Long txBranchId, String applicationName) {

    }

    @Override
    public void doCommit(Long txGroupId, Long txBranchId) {

    }

    @Override
    public void doRollback(Long txGroupId) {

    }

    @Override
    public Long getTransactionBranchId(Long txGroupId, String applicationName) {
        return null;
    }

    @Override
    public Long getTransactionGroupId() {
        return null;
    }

}

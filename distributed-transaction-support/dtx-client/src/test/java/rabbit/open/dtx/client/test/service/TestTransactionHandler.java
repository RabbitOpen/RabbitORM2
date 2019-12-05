package rabbit.open.dtx.client.test.service;

import rabbit.open.dtx.client.net.TransactionHandler;

/**
 * @author xiaoqianbin
 * @date 2019/12/5
 **/
public class TestTransactionHandler implements TransactionHandler {
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
}

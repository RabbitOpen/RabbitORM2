package rabbit.open.dtx.client.net;

import rabbit.open.dtx.common.nio.client.MessageListener;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;

/**
 * 异步消息处理器， 同步消息会阻塞其它请求的数据读取
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class DtxMessageListener extends MessageListener {

    private TransactionMessageHandler tmh = new TransactionMessageHandler();

    private AbstractTransactionManager transactionManger;

    public DtxMessageListener(AbstractTransactionManager transactionManger) {
        this.transactionManger = transactionManger;
    }

    @Override
    protected int getMaxThreadSize() {
        return 5;
    }

    @Override
    protected int getCoreSize() {
        return 3;
    }

    @Override
    protected int getQueueSize() {
        return 1000;
    }

    @Override
    protected void rollback(String applicationName, Long txGroupId, Long txBranchId) {
        tmh.rollback(applicationName, txGroupId, txBranchId);
    }

    @Override
    protected void commit(String applicationName, Long txGroupId, Long txBranchId) {
        tmh.commit(applicationName, txGroupId, txBranchId);
    }

    @Override
    protected AbstractTransactionManager getTransactionManger() {
        return transactionManger;
    }
}

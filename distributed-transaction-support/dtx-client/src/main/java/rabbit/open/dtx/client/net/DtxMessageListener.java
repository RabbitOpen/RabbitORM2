package rabbit.open.dtx.client.net;

import rabbit.open.dtx.common.nio.client.AbstractMessageListener;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.pub.CallHelper;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 异步消息处理器， 同步消息会阻塞其它请求的数据读取
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class DtxMessageListener extends AbstractMessageListener {

    private TransactionMessageHandler tmh = new TransactionMessageHandler();

    private AbstractTransactionManager transactionManger;

    private Thread sweeper;

    private Semaphore semaphore = new Semaphore(0);

    public DtxMessageListener(AbstractTransactionManager transactionManger) {
        this.transactionManger = transactionManger;
        sweeper = new Thread(() -> {
            while (true) {
                commit();
                try {
                    if (semaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                        break;
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        sweeper.setDaemon(true);
        sweeper.start();
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
    protected boolean rollback(String applicationName, Long txGroupId, Long txBranchId) {
        return tmh.rollback(applicationName, txGroupId, txBranchId);
    }

    @Override
    protected boolean commit() {
        return tmh.commit();
    }

    @Override
    protected AbstractTransactionManager getTransactionManger() {
        return transactionManger;
    }

    @Override
    public void close() {
        super.close();
        semaphore.release();
        CallHelper.ignoreExceptionCall(() -> sweeper.join());
    }
}

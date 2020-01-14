package rabbit.open.dtx.common.nio.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.pub.inter.TransactionHandler;
import rabbit.open.dtx.common.nio.pub.protocol.CommitMessage;
import rabbit.open.dtx.common.nio.pub.protocol.RollBackMessage;

import java.io.Closeable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 消息监听接口
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public abstract class AbstractMessageListener implements MessageListener<CommitMessage>, Closeable {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private ThreadPoolExecutor tpe;

    public AbstractMessageListener() {
        tpe = new ThreadPoolExecutor(getCoreSize(), getMaxThreadSize(), 10,
                TimeUnit.MINUTES, new ArrayBlockingQueue<>(getQueueSize()), (r, executor) -> {
            logger.error("too many message is received");
            r.run();
        });
    }

    protected abstract int getMaxThreadSize();

    protected abstract int getCoreSize();

    protected abstract int getQueueSize();

    protected abstract boolean rollback(String applicationName, Long txGroupId, Long txBranchId);

    protected abstract boolean commit();

    protected abstract AbstractTransactionManager getTransactionManger();

    public final void onMessageReceived(CommitMessage msg) {
        tpe.submit(() -> {
            TransactionHandler transactionHandler = getTransactionManger().getTransactionHandler();
            try {
                if (msg instanceof RollBackMessage) {
                    rollback(msg.getApplicationName(), msg.getTxGroupId(), msg.getTxBranchId());
                    transactionHandler.confirmBranchRollback(msg.getApplicationName(), msg.getTxGroupId(), msg.getTxBranchId());
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void close() {
        tpe.shutdown();
    }

}

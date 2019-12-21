package rabbit.open.dtx.common.nio.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;
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
public abstract class AbstractMessageListener implements MessageListener, Closeable {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ThreadPoolExecutor tpe;

    public AbstractMessageListener() {
        tpe = new ThreadPoolExecutor(getCoreSize(), getMaxThreadSize(), 10,
                TimeUnit.MINUTES, new ArrayBlockingQueue<>(getQueueSize()), (r, executor) -> {
            // 多余的消息直接丢弃
        });
    }

    protected abstract int getMaxThreadSize();

    protected abstract int getCoreSize();

    protected abstract int getQueueSize();

    protected abstract boolean rollback(String applicationName, Long txGroupId, Long txBranchId);

    protected abstract boolean commit(String applicationName, Long txGroupId, Long txBranchId);

    protected abstract AbstractTransactionManager getTransactionManger();

    public final void onMessageReceived(Object msg) {
        tpe.submit(() -> {
            TransactionHandler transactionHandler = getTransactionManger().getTransactionHandler();
            CommitMessage cm;
            try {
                if (msg instanceof RollBackMessage) {
                    cm = (RollBackMessage) msg;
                    rollback(cm.getApplicationName(), cm.getTxGroupId(), cm.getTxBranchId());
                    transactionHandler.confirmBranchRollback(cm.getApplicationName(),cm.getTxGroupId(), cm.getTxBranchId());
                } else if (msg instanceof CommitMessage) {
                    cm = (CommitMessage) msg;
                    commit(cm.getApplicationName(), cm.getTxGroupId(), cm.getTxBranchId());
                    transactionHandler.confirmBranchCommit(cm.getApplicationName(),cm.getTxGroupId(), cm.getTxBranchId());
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

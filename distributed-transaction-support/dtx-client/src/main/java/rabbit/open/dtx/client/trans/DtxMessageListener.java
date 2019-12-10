package rabbit.open.dtx.client.trans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.client.net.TransactionMessageHandler;
import rabbit.open.dtx.common.nio.client.MessageListener;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.nio.pub.protocol.CommitMessage;
import rabbit.open.dtx.common.nio.pub.protocol.RollBackMessage;

/**
 * 消息处理器
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class DtxMessageListener implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    TransactionMessageHandler tmh = new TransactionMessageHandler();

    AbstractTransactionManger transactionManger;

    public DtxMessageListener(AbstractTransactionManger transactionManger) {
        this.transactionManger = transactionManger;
    }

    @Override
    public void onMessageReceived(Object msg) {
        TransactionHandler transactionHandler = transactionManger.getTransactionHandler();
        CommitMessage cm;
        try {
            if (msg instanceof RollBackMessage) {
                cm = (RollBackMessage) msg;
                tmh.rollback(cm.getApplicationName(), cm.getTxGroupId(), cm.getTxBranchId());
                transactionHandler.confirmBranchRollback(cm.getTxGroupId(), cm.getTxBranchId());
            } else if (msg instanceof CommitMessage) {
                cm = (CommitMessage) msg;
                tmh.commit(cm.getApplicationName(), cm.getTxGroupId(), cm.getTxBranchId());
                transactionHandler.confirmBranchCommit(cm.getTxGroupId(), cm.getTxBranchId());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}

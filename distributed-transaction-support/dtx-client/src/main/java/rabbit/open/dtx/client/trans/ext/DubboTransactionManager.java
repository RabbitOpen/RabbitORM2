package rabbit.open.dtx.client.trans.ext;

import com.alibaba.dubbo.rpc.RpcContext;
import org.springframework.util.StringUtils;
import rabbit.open.dtx.client.context.DistributedTransactionContext;
import rabbit.open.dtx.client.trans.AbstractTransactionManger;
import rabbit.open.dtx.client.trans.DtxMessageListener;
import rabbit.open.dtx.common.nio.client.DistributedTransactionObject;
import rabbit.open.dtx.common.nio.client.MessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 针对dubbo的事务管理器
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class DubboTransactionManager extends AbstractTransactionManger {

    public static final String TRANSACTION_GROUP_ID = "_DTX_TRANSACTION_GROUP_ID";

    private TransactionHandler transactionHandler;

    private MessageListener messageListener = new DtxMessageListener(this);

    private List<Node> nodes;

    private String applicationName;

    private int maxConcurrenceSize;

    @Override
    public boolean isTransactionOpen(Method method) {
        if (super.isTransactionOpen(method)) {
            return true;
        }
        String txGroupId = RpcContext.getContext().getAttachment(TRANSACTION_GROUP_ID);
        if (StringUtils.isEmpty(txGroupId)) {
            return false;
        }
        DistributedTransactionObject tranObj = new DistributedTransactionObject(Long.parseLong(txGroupId));
        tranObj.setPromoter(false);
        tranObj.setTransactionOwner(method);
        DistributedTransactionContext.setDistributedTransactionObject(tranObj);
        return true;
    }

    @Override
    protected TransactionHandler getTransactionHandler() {
        return transactionHandler;
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public MessageListener getMessageListener() {
        return messageListener;
    }

    @Override
    public int getMaxConcurrenceSize() {
        return maxConcurrenceSize;
    }

    @Override
    public List<Node> getServerNodes() {
        return nodes;
    }

}

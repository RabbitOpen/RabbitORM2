package rabbit.open.dtx.client.rpc.support;

import com.alibaba.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import rabbit.open.dtx.client.net.DtxMessageListener;
import rabbit.open.dtx.common.context.DistributedTransactionContext;
import rabbit.open.dtx.common.nio.client.DistributedTransactionObject;
import rabbit.open.dtx.common.nio.client.MessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 针对dubbo的事务管理器
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class DubboTransactionManager extends AbstractTransactionManager {

    private transient Logger logger = LoggerFactory.getLogger(getClass());

    public static final String TRANSACTION_GROUP_ID = "_DTX_TRANSACTION_GROUP_ID";

    protected transient MessageListener messageListener = new DtxMessageListener(this);

    private String hosts;

    private String applicationName;

    private int maxConcurrenceSize;

    private long rpcTimeoutSeconds = 3L;

    public DubboTransactionManager(String hosts, String applicationName, int maxConcurrenceSize) {
        this.hosts = hosts;
        this.applicationName = applicationName;
        this.maxConcurrenceSize = maxConcurrenceSize;
    }

    @Override
    public boolean isTransactionOpen(Method method) {
        if (super.isTransactionOpen(method)) {
            return true;
        }
        String txGroupId = RpcContext.getContext().getAttachment(TRANSACTION_GROUP_ID);
        if (StringUtils.isEmpty(txGroupId)) {
            logger.debug("open original transaction");
            return false;
        }
        logger.debug("'{}' open nested transaction, txGroupId is '{}'", method.getName(), txGroupId);
        DistributedTransactionObject tranObj = new DistributedTransactionObject(Long.parseLong(txGroupId));
        tranObj.setPromoter(false);
        tranObj.setTransactionOwner(method);
        DistributedTransactionContext.setDistributedTransactionObject(tranObj);
        return true;
    }

    @Override
    protected long getRpcTimeoutSeconds() {
        return rpcTimeoutSeconds;
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
        List<Node> list = new ArrayList<>();
        for (String host : hosts.split(",")) {
            String[] hp = host.trim().split(":");
            list.add(new Node(hp[0].trim(), Integer.parseInt(hp[1].trim())));
        }
        return list;
    }

    public void setRpcTimeoutSeconds(long rpcTimeoutSeconds) {
        this.rpcTimeoutSeconds = rpcTimeoutSeconds;
    }
}

package rabbit.open.dtx.common.test.rpc;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.nio.client.AbstractMessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 自定义一个事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Component("rpcTransactionManger")
@SuppressWarnings("serial")
public class RpcTransactionManager extends AbstractTransactionManager {


    @Override
    protected long getRpcTimeoutSeconds() {
        return 2;
    }

    @Override
    public String getApplicationName() {
        return "rpcTransactionManger";
    }

    @Override
    public AbstractMessageListener getMessageListener() {
        return null;
    }

    @Override
    public List<Node> getServerNodes() {
        return Arrays.asList(new Node("localhost", 10086));
    }

    @Override
    public void init() {
        // TO DO : DO nothing
    }

    public void  manualInit() throws IOException {
        super.init();
    }
}

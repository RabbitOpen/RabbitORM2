package rabbit.open.dtx.common.test;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.nio.client.AbstractMessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义一个事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Component("testTransactionManger")
@SuppressWarnings("serial")
public class TestTransactionManager extends AbstractTransactionManager {

    @Override
    public boolean isTransactionOpen(Method method) {
        return false;
    }

    @Override
    protected long getRpcTimeoutSeconds() {
        return 3L;
    }

    @Override
    public String getApplicationName() {
        return "test-app";
    }

    @Override
    public AbstractMessageListener getMessageListener() {
        return null;
    }

    @Override
    public void init() throws IOException {

    }

    @Override
    public List<Node> getServerNodes() {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            nodes.add(new Node("localhost", 10000));
        }
        return nodes;
    }
}

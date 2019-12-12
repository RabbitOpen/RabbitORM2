package rabbit.open.dtx.common.test.rpc;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 自定义一个事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Component("MemoryTransactionManger")
@SuppressWarnings("serial")
public class MemoryTransactionManger extends AbstractTransactionManger {


    @Override
    protected long getDefaultTimeoutSeconds() {
        return 2;
    }

    @Override
    public String getApplicationName() {
        return "MemoryTransactionManger";
    }

    @Override
    public List<Node> getServerNodes() {
        return Arrays.asList(new Node("localhost", 10021));
    }

    @Override
    public void init() {
    }

    public void  manualInit() throws IOException {
        super.init();
    }
}

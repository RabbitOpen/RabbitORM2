package rabbit.open.dtx.common.test.rpc;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.nio.client.DistributedTransactionManger;
import rabbit.open.dtx.common.nio.client.DistributedTransactionObject;
import rabbit.open.dtx.common.nio.client.Node;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 自定义一个事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Component("rpcTransactionManger")
@SuppressWarnings("serial")
public class RpcTransactionManger implements DistributedTransactionManger {


    @Override
    public void beginTransaction(Method method) {

    }

    @Override
    public void rollback(Method method) {

    }

    @Override
    public void commit(Method method) {

    }

    @Override
    public boolean isTransactionOpen() {
        return false;
    }

    @Override
    public DistributedTransactionObject getCurrentTransactionObject() {
        return null;
    }

    @Override
    public Long getTransactionBranchId() {
        return null;
    }

    @Override
    public Long getTransactionGroupId() {
        return null;
    }

    @Override
    public String getApplicationName() {
        return "rpcTransactionManger";
    }

    @Override
    public List<Node> getServerNodes() {
        return Arrays.asList(new Node("localhost", 10086));
    }
}

package rabbit.open.dtx.common.test;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.nio.client.DistributedTransactionManager;
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
@Component("testTransactionManger")
@SuppressWarnings("serial")
public class TestTransactionManager implements DistributedTransactionManager {


    @Override
    public void beginTransaction(Method method) {

    }

    @Override
    public void rollback(Method method, long timeoutSeconds) {

    }

    @Override
    public void commit(Method method) {

    }

    @Override
    public boolean isTransactionOpen(Method method) {
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
        return "test-app";
    }

    @Override
    public List<Node> getServerNodes() {
        return Arrays.asList(
                new Node("localhost", 10000),
                new Node("localhost", 10000),
                new Node("localhost", 10000),
                new Node("localhost", 10000),
                new Node("localhost", 10000));
    }
}

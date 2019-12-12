package rabbit.open.dtx.client.test.service;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义一个事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Component
@SuppressWarnings("serial")
public class SimpleTransactionManager extends AbstractTransactionManager {

    AtomicLong idGenerator = new AtomicLong(0);

    private long lastBranchId;

    // 事务消息处理器
    private TransactionHandler transactionHandler = new TransactionHandler() {

        @Override
        public Long getTransactionBranchId(Long txGroupId, String applicationName) {
            // 这个id应该从服务端统一获取
            return idGenerator.getAndAdd(1L);
        }

        @Override
        public Long getTransactionGroupId(String applicationName) {
            return idGenerator.getAndAdd(1L);
        }
    };

    @Override
    public TransactionHandler getTransactionHandler() {
        return transactionHandler;
    }

    @Override
    protected long getRpcTimeoutSeconds() {
        return 3L;
    }

    @Override
    protected void doCommit(Method method) {
        super.doCommit(method);
        lastBranchId = getTransactionBranchId();
    }

    // 测试SupportTest类专用
    public long getLastBranchId() {
        return lastBranchId;
    }

    @Override
    public String getApplicationName() {
        return "test-app";
    }

    @Override
    public void init() throws IOException {

    }
}

package rabbit.open.dtx.client.test.service;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.client.context.ext.AbstractTransactionManger;
import rabbit.open.dtx.client.enhance.ext.DistributedTransactionObject;
import rabbit.open.dtx.client.net.TransactionHandler;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义一个事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Component
@SuppressWarnings("serial")
public class SimpleTransactionManger extends AbstractTransactionManger {

    AtomicLong idGenerator = new AtomicLong(0);

    // 事务消息处理器
    private TransactionHandler transactionHandler = new TestTransactionHandler() {

        @Override
        public Long getTransactionBranchId(Long txGroupId, String applicationName) {
            // 这个id应该从服务端统一获取
            return idGenerator.getAndAdd(1L);
        }
    };

    @Override
    protected TransactionHandler getTransactionHandler() {
        return transactionHandler;
    }

    private long lastBranchId;

    @Override
    public DistributedTransactionObject newTransactionObject() {
        return new DistributedTransactionObject(idGenerator.getAndAdd(1L));
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
}

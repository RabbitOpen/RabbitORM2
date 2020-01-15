package rabbit.open.dtx.server.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import rabbit.open.dtx.client.net.DtxMessageListener;
import rabbit.open.dtx.common.context.DistributedTransactionContext;
import rabbit.open.dtx.common.exception.DistributedTransactionException;
import rabbit.open.dtx.common.nio.client.AbstractMessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.inter.TransactionHandler;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;
import rabbit.open.dtx.server.DtxServerWrapper;
import rabbit.open.dtx.server.handler.MemoryTransactionHandler;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 相关逻辑测试
 * @author xiaoqianbin
 * @date 2019/12/12
 **/
@ContextConfiguration(locations = {"classpath:server.xml"})
public class ClientLogicalTest {

    @Test
    public void transactionTest() throws IOException, NoSuchMethodException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        DtxServerEventHandler serverEventHandler = new DtxServerEventHandler();
        serverEventHandler.setBossCoreSize(1);
        serverEventHandler.setMaxBossConcurrence(2);
        serverEventHandler.setMaxBossQueueSize(10);
        MemoryTransactionHandler memTxHandler = new MemoryTransactionHandler();
        serverEventHandler.setTransactionHandler(memTxHandler);
        serverEventHandler.init();
        DtxServerWrapper serverWrapper = new DtxServerWrapper(21118, serverEventHandler);

        DistributedTransactionContext.clear();
        TestTransactionManager manager = new TestTransactionManager();
        manager.init();
        Method method = HelloService.class.getDeclaredMethod("hello4");
        manager.beginTransaction(method);
        // 事务处理器
        TransactionHandler handler = manager.getTransactionHandler();
        Long txGroupId = manager.getCurrentTransactionObject().getTxGroupId();
        Long branchId = handler.getTransactionBranchId(txGroupId, manager.getApplicationName());
        handler.doBranchCommit(txGroupId, branchId, manager.getApplicationName());
        assertWrongCommit(handler, txGroupId);
        assertWrongRollback(handler, txGroupId);
        // 空回滚
        handler.doRollback(handler.getTransactionGroupId("empty-app"), "empty-app");

        TestCase.assertEquals(1, memTxHandler.getOpenedTransactionCount());
        manager.commit(method);
        DistributedTransactionContext.clear();

        Semaphore semaphore = new Semaphore(0);
        if (semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            // 等100ms，保证response已经回来了
            throw new RuntimeException("见鬼了");
        }
        // 等待response
        TestCase.assertEquals(0, memTxHandler.getOpenedTransactionCount());

        // 测试回滚
        manager.beginTransaction(method);
        txGroupId = manager.getCurrentTransactionObject().getTxGroupId();
        branchId = handler.getTransactionBranchId(txGroupId, manager.getApplicationName());
        handler.doBranchCommit(txGroupId, branchId, manager.getApplicationName());
        TestCase.assertEquals(1, memTxHandler.getOpenedTransactionCount());
        manager.rollback(method, 10);
        if (semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            // 等100ms，保证response已经回来了
            throw new RuntimeException("见鬼了");
        }
        DistributedTransactionContext.clear();
        // 等待response
        TestCase.assertEquals(0, memTxHandler.getOpenedTransactionCount());

        TestCase.assertEquals(1, serverWrapper.getServer().getServerAgentMonitor().getAgents().size());
        // 模拟清理线程
        Field lastActiveTime = ChannelAgent.class.getDeclaredField("lastActiveTime");
        lastActiveTime.setAccessible(true);
        for (ChannelAgent agent : serverWrapper.getServer().getServerAgentMonitor().getAgents()) {
            lastActiveTime.set(agent, 0L);
        }
        serverWrapper.getServer().getServerAgentMonitor().interrupt();
        holdOn(100);
        // 连接被主动移除
        TestCase.assertEquals(0, serverWrapper.getServer().getServerAgentMonitor().getAgents().size());

        manager.destroy();
        //关闭服务端
        serverWrapper.close();
    }

    private void assertWrongRollback(TransactionHandler handler, Long txGroupId) {
        try {
            handler.doRollback(txGroupId, "my-app");
            throw new RuntimeException("不能走到这一步");
        } catch (Exception e) {
            TestCase.assertEquals(DistributedTransactionException.class, e.getClass());
        }
    }

    private void assertWrongCommit(TransactionHandler handler, Long txGroupId) {
        try {
            handler.doCommit(txGroupId, null, "my-app");
            throw new RuntimeException("不能走到这一步");
        } catch (Exception e) {
            TestCase.assertEquals(DistributedTransactionException.class, e.getClass());
        }
    }

    private void holdOn(long milliSeconds) throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        if (semaphore.tryAcquire(milliSeconds, TimeUnit.MILLISECONDS)) {
            // 等100ms，保证response已经回来了
            throw new RuntimeException("见鬼了");
        }
    }

    @SuppressWarnings("serial")
	class TestTransactionManager extends AbstractTransactionManager {

        @Override
        protected long getRpcTimeoutSeconds() {
            return 3;
        }

        @Override
        public AbstractMessageListener getMessageListener() {
            return new DtxMessageListener(this);
        }

        @Override
        public String getApplicationName() {
            return "logicalX-test-app";
        }

        @Override
        public List<Node> getServerNodes() {
            return Arrays.asList(new Node("localhost", 21118));
        }
    }

}

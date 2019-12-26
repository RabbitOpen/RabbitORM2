package rabbit.open.dtx.common.test.logical;

import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import rabbit.open.dtx.client.net.DtxMessageListener;
import rabbit.open.dtx.client.test.service.ProductService;
import rabbit.open.dtx.common.nio.client.AbstractMessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.exception.DistributedTransactionException;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;
import rabbit.open.dtx.common.nio.server.DtxServerWrapper;
import rabbit.open.dtx.common.nio.server.MemoryTransactionHandler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 相关逻辑测试
 * @author xiaoqianbin
 * @date 2019/12/12
 **/
@ContextConfiguration(locations = {"classpath:common-support.xml"})
public class LogicalTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void transactionTest() throws IOException, NoSuchMethodException, InterruptedException {
        DtxServerEventHandler serverEventHandler = new DtxServerEventHandler();
        serverEventHandler.setBossCoreSize(1);
        serverEventHandler.setMaxBossConcurrence(2);
        serverEventHandler.setMaxBossQueueSize(10);
        MemoryTransactionHandler memTxHandler = new MemoryTransactionHandler();
        serverEventHandler.setTransactionHandler(memTxHandler);
        serverEventHandler.init();
        DtxServerWrapper serverWrapper = new DtxServerWrapper(20118, serverEventHandler);

        TestTransactionManager manager = new TestTransactionManager();

        manager.setListener(new DtxMessageListener(manager) {

        });

        manager.init();

        Method method = ProductService.class.getDeclaredMethod("jdbcAdd");
        manager.beginTransaction(method);

        // 事务处理器
        TransactionHandler handler = manager.getTransactionHandler();
        Long txGroupId = manager.getCurrentTransactionObject().getTxGroupId();
        Long branchId = handler.getTransactionBranchId(txGroupId, manager.getApplicationName());
        handler.doBranchCommit(txGroupId, branchId, manager.getApplicationName());

        try {
            handler.doCommit(txGroupId, null, "my-app");
            throw new RuntimeException("不可能走到这一步");
        } catch (Exception e) {
            TestCase.assertEquals(DistributedTransactionException.class, e.getClass());
        }
        TestCase.assertEquals(1, memTxHandler.getOpenedTransactionCount());
        manager.commit(method);
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
        // 等待response
        TestCase.assertEquals(0, memTxHandler.getOpenedTransactionCount());
        manager.destroy();
        //关闭服务端
        serverWrapper.close();
    }

    @SuppressWarnings("serial")
    class TestTransactionManager extends AbstractTransactionManager {

        @Override
        protected long getRpcTimeoutSeconds() {
            return 3;
        }

        AbstractMessageListener listener = new AbstractMessageListener() {

            @Override
            protected int getMaxThreadSize() {
                return 1;
            }

            @Override
            protected int getCoreSize() {
                return 1;
            }

            @Override
            protected int getQueueSize() {
                return 10;
            }

            @Override
            protected boolean rollback(String applicationName, Long txGroupId, Long txBranchId) {
                logger.info("doRollback");
                return true;
            }

            @Override
            protected boolean commit(String applicationName, Long txGroupId, Long txBranchId) {
                logger.info("doCommit");
                return true;
            }

            @Override
            protected AbstractTransactionManager getTransactionManger() {
                return TestTransactionManager.this;
            }
        };

        public void setListener(AbstractMessageListener listener) {
            this.listener = listener;
        }

        @Override
        public AbstractMessageListener getMessageListener() {
            return listener;
        }

        @Override
        public String getApplicationName() {
            return "logical-test-app";
        }

        @Override
        public List<Node> getServerNodes() {
            return Arrays.asList(new Node("localhost", 20118));
        }
    }

    /**
     * 锁数据测试
     * @author xiaoqianbin
     * @date 2019/12/24
     **/
    @Test
    public void lockDataTest() throws IOException, NoSuchMethodException, InterruptedException {
        DtxServerEventHandler serverEventHandler = new DtxServerEventHandler();
        MemoryTransactionHandler memTxHandler = new MemoryTransactionHandler();
        serverEventHandler.setTransactionHandler(memTxHandler);
        serverEventHandler.init();
        DtxServerWrapper serverWrapper = new DtxServerWrapper(20119, serverEventHandler);
        TestTransactionManager manager = new TestTransactionManager() {
            @Override
            public List<Node> getServerNodes() {
                return Arrays.asList(new Node("localhost", 20119));
            }

            @Override
            public String getApplicationName() {
                return "lock-data-app";
            }

        };
        manager.init();

        Method method = ProductService.class.getDeclaredMethod("jdbcAdd");
        manager.beginTransaction(method);
        // 事务处理器
        TransactionHandler handler = manager.getTransactionHandler();
        Long txGroupId = manager.getCurrentTransactionObject().getTxGroupId();
        Long branchId = handler.getTransactionBranchId(txGroupId, manager.getApplicationName());
        List<String> list = new ArrayList<>();
        list.add("lock-1");
        handler.lockData(manager.getApplicationName(), txGroupId, branchId, list);
        list.add("lock-2");
        handler.lockData(manager.getApplicationName(), txGroupId, branchId, list);
        handler.doBranchCommit(txGroupId, branchId, manager.getApplicationName());

        String app = manager.getApplicationName();
        new Thread(() -> {
            Long g2 = handler.getTransactionGroupId(app);
            Long b2 = handler.getTransactionBranchId(g2, app);
            handler.lockData(app, g2, b2, list);
            handler.doCommit(g2, b2, app);
        }).start();
        holdOn(1000);
        manager.commit(method);

        holdOn(200);
        // 等待response
        TestCase.assertEquals(0, memTxHandler.getOpenedTransactionCount());
        manager.destroy();
        //关闭服务端
        serverWrapper.close();
    }

    private void holdOn(long milliSeconds) throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        if (semaphore.tryAcquire(milliSeconds, TimeUnit.MILLISECONDS)) {
            // 等100ms，保证response已经回来了
            throw new RuntimeException("见鬼了");
        }
    }

    @Test
    public void benchmark() throws InterruptedException {
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < 100000; i++) {
//            new String("abc-adsfafdsfasdfsafsafadsfsfsf" + i).intern();
//        }
//        System.out.println("cost " + (System.currentTimeMillis() - start));

        int count = 100;
        CountDownLatch cdl = new CountDownLatch(count);
        ReentrantLock lock = new ReentrantLock();
        for (int i = 0; i < count; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100000; j++) {
                        try {
                            lock.lock();
                        } finally {
                            lock.unlock();
                        }
                    }
                    cdl.countDown();
                }
            }).start();
        }
        cdl.await();
    }

}

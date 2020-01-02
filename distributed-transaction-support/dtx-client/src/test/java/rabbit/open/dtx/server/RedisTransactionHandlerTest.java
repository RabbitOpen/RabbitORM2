package rabbit.open.dtx.server;

import junit.framework.TestCase;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.dtx.client.test.service.ProductService;
import rabbit.open.dtx.common.nio.client.AbstractMessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.exception.DistributedTransactionException;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.nio.server.*;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 基于redis的事务管理器
 * @author xiaoqianbin
 * @date 2020/1/2
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:server.xml")
public class RedisTransactionHandlerTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource(name = "redisTransactionHandler")
    private RedisTransactionHandler redisTransactionHandler;

    private JedisPool getPool() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(3);
        JedisPool pool = new JedisPool(poolConfig, "localhost", 6379);
        return pool;
    }

    @Test
    public void redisTransactionHandlerTest() throws IOException, NoSuchMethodException, InterruptedException {
        PooledJedisClient jedisClient = new PooledJedisClient(getPool());
        Long count = jedisClient.zcount(RedisKeyNames.DTX_CONTEXT_LIST.name(), 0, Long.MAX_VALUE);
        redisTransactionHandler.setJedisClient(jedisClient);
        DtxServerEventHandler serverEventHandler = new DtxServerEventHandler();
        serverEventHandler.setTransactionHandler(redisTransactionHandler);
        serverEventHandler.init();
        DtxServerWrapper serverWrapper = new DtxServerWrapper(12345, serverEventHandler);
        TestTransactionManager manager = new TestTransactionManager();
        manager.init();
        // 事务处理器
        Method method = ProductService.class.getDeclaredMethod("jdbcAdd");
        manager.beginTransaction(method);
        TransactionHandler handler = manager.getTransactionHandler();
        Long txGroupId = manager.getCurrentTransactionObject().getTxGroupId();
        Long branchId = handler.getTransactionBranchId(txGroupId, manager.getApplicationName());
        handler.doBranchCommit(txGroupId, branchId, manager.getApplicationName());
        // 验证错误的提交和回滚操作
        assertWrongCommitAndRollback(handler, txGroupId);
        // 提交
        handler.doCommit(txGroupId, null, manager.getApplicationName());

        // 空回滚
        txGroupId = handler.getTransactionGroupId(manager.getApplicationName());
        handler.doRollback(txGroupId, manager.getApplicationName());
        // 空提交
        txGroupId = handler.getTransactionGroupId(manager.getApplicationName());
        handler.doCommit(txGroupId, null, manager.getApplicationName());

        // 正常多分支提交
        txGroupId = handler.getTransactionGroupId(manager.getApplicationName());
        branchId = handler.getTransactionBranchId(txGroupId, manager.getApplicationName());
        handler.doBranchCommit(txGroupId, branchId, manager.getApplicationName());
        branchId = handler.getTransactionBranchId(txGroupId, manager.getApplicationName());
        handler.doBranchCommit(txGroupId, branchId, manager.getApplicationName());
        handler.doCommit(txGroupId, null, manager.getApplicationName());

        // 正常多分支回滚
        txGroupId = handler.getTransactionGroupId(manager.getApplicationName());
        branchId = handler.getTransactionBranchId(txGroupId, manager.getApplicationName());
        handler.doBranchCommit(txGroupId, branchId, manager.getApplicationName());
        branchId = handler.getTransactionBranchId(txGroupId, manager.getApplicationName());
        handler.doBranchCommit(txGroupId, branchId, manager.getApplicationName());
        handler.doRollback(txGroupId, manager.getApplicationName());

        manager.destroy();
        serverWrapper.close();
        TestCase.assertEquals(count, jedisClient.zcount(RedisKeyNames.DTX_CONTEXT_LIST.name(), 0, Long.MAX_VALUE));
        redisTransactionHandler.destroy();
    }

    private void holdOn(long milliSeconds) throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        if (semaphore.tryAcquire(milliSeconds, TimeUnit.MILLISECONDS)) {
            // 等100ms，保证response已经回来了
            throw new RuntimeException("见鬼了");
        }
    }

    private void assertWrongCommitAndRollback(TransactionHandler handler, Long txGroupId) {
        try {
            handler.doCommit(txGroupId, null, "my-app");
            throw new RuntimeException("不可能走到这一步");
        } catch (Exception e) {
            TestCase.assertEquals(DistributedTransactionException.class, e.getClass());
        }
        try {
            handler.doRollback(txGroupId, "my-app");
            throw new RuntimeException("不可能走到这一步");
        } catch (Exception e) {
            TestCase.assertEquals(DistributedTransactionException.class, e.getClass());
        }
    }

    @SuppressWarnings("serial")
    class TestTransactionManager extends AbstractTransactionManager {

        public DtxChannelAgentPool getPool() {
            return pool;
        }

        @Override
        protected long getRpcTimeoutSeconds() {
            return 3000;
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
            return "redis-dtx-test-app";
        }

        @Override
        public List<Node> getServerNodes() {
            return Arrays.asList(new Node("127.0.0.1", 12345));
        }
    }
}

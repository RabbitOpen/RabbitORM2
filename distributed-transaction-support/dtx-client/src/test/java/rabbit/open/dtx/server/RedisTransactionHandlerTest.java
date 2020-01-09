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
import rabbit.open.dtx.common.context.DistributedTransactionContext;
import rabbit.open.dtx.common.exception.DeadLockException;
import rabbit.open.dtx.common.exception.DistributedTransactionException;
import rabbit.open.dtx.common.nio.client.AbstractMessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.nio.server.*;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
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
    public void luaTest() throws InterruptedException {
        PooledJedisClient jedisClient = new PooledJedisClient(getPool());
        jedisClient.del("map");
        TestCase.assertTrue(1L == jedisClient.casHset("map", "k1", "v1", "v2"));
        TestCase.assertTrue(1L == jedisClient.casHset("map", "k1", "v1", "v2"));
        TestCase.assertEquals(jedisClient.hget("map", "k1"), "v1");
        jedisClient.del("map");
        jedisClient.hset("map", "k1", "v2");
        TestCase.assertTrue(0 == jedisClient.casHset("map", "k1", "v1", "v2"));
        TestCase.assertTrue(0 == jedisClient.casHset("map", "k1", "v1", "v2"));
        TestCase.assertEquals(jedisClient.hget("map", "k1"), "v2");

        jedisClient.del("list");
        jedisClient.rpush("list", "hello", "kitty", "world", "apple");

        PopInfo info = jedisClient.casLpop("list");
        TestCase.assertEquals("hello", info.getResult());
        TestCase.assertEquals("kitty", info.getNext());
        info = jedisClient.casLpop("list");
        TestCase.assertEquals("kitty", info.getResult());
        TestCase.assertEquals("world", info.getNext());

        info = jedisClient.casLpop("list");
        info = jedisClient.casLpop("list");
        TestCase.assertEquals("apple", info.getResult());
        TestCase.assertNull(info.getNext());

        jedisClient.del("map2");
        jedisClient.hset("map2", "k1", "v1");
        Map<String, String> map = jedisClient.hsetGetAll("map2", "k2", "v2");
        TestCase.assertEquals(2, map.size());
        TestCase.assertEquals("v1", map.get("k1"));
        TestCase.assertEquals("v2", map.get("k2"));

        map = jedisClient.hgetAllAndDel("map2");
        TestCase.assertTrue(0 != map.size());
        // 节点已经删除了
        TestCase.assertTrue(0 == jedisClient.hgetAll("map2").size());

        jedisClient.zadd("zset", System.currentTimeMillis(), "k1");
        Set<String> set = jedisClient.zrangeByScore("zset", 0, System.currentTimeMillis() - 100);
        TestCase.assertTrue(set.isEmpty());
        holdOn(100);
        TestCase.assertTrue(!jedisClient.zrangeByScore("zset", 0, System.currentTimeMillis() - 100).isEmpty());


        jedisClient.close();
    }

    @Test
    public void redisTransactionHandlerTest() throws IOException, NoSuchMethodException, InterruptedException {
        PooledJedisClient jedisClient = new PooledJedisClient(getPool());
        Long count = jedisClient.zcount(RedisKeyNames.DTX_CONTEXT_LIST.name(), 0, Long.MAX_VALUE);
        // 模拟死掉的context
        TestTransactionManager manager = new TestTransactionManager();
        mockDeadContext(jedisClient, manager, -200);
        redisTransactionHandler.setJedisClient(jedisClient);
        redisTransactionHandler.startSweeper();
        List<DtxServerWrapper> serverWrappers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            serverWrappers.add(createServer(12345 + i));
        }
        manager.init();
        // 事务处理器
        Method method = ProductService.class.getDeclaredMethod("jdbcAdd");
        manager.beginTransaction(method);
        TransactionHandler handler = manager.getTransactionHandler();
        Long txGroupId = manager.getCurrentTransactionObject().getTxGroupId();
        String appName = manager.getApplicationName();
        Long branchId = handler.getTransactionBranchId(txGroupId, appName);
        handler.doBranchCommit(txGroupId, branchId, appName);
        // 验证错误的提交和回滚操作
        assertWrongCommitAndRollback(handler, txGroupId);
        // 提交
        handler.doCommit(txGroupId, null, appName);

        mockDeadContext(jedisClient, manager, -100);
        // 空事务测试
        emptyTransactionTest(handler, appName);
        // 正常提交回滚测试
        normalRollbackAndCommitTest(handler, appName);

        // 资源锁测试
        lockDataTest(manager, method, handler, appName);

        // 死锁测试
        deadLockTest(manager, method, handler, appName);

        // 销毁资源信息
        manager.destroy();
        for (DtxServerWrapper serverWrapper : serverWrappers) {
            serverWrapper.close();
        }
        TestCase.assertEquals(count, jedisClient.zcount(RedisKeyNames.DTX_CONTEXT_LIST.name(), 0, Long.MAX_VALUE));
        redisTransactionHandler.destroy();
    }

    private void lockDataTest(TestTransactionManager manager, Method method, TransactionHandler handler, String appName) throws InterruptedException {
        DistributedTransactionContext.clear();
        manager.beginTransaction(method);
        Long txGroupId = manager.getCurrentTransactionObject().getTxGroupId();
        Long branchId = handler.getTransactionBranchId(txGroupId, appName);
        List<String> locks = new ArrayList<>();
        locks.add("data-1");
        locks.add("data-2");
        handler.lockData(appName, txGroupId, branchId, locks);
        locks = new ArrayList<>();
        locks.add("data-2");
        locks.add("data-3");
        handler.lockData(appName, txGroupId, branchId, locks);
        Semaphore semaphore = new Semaphore(0);
        new Thread(() -> {
            Long gId = handler.getTransactionGroupId(appName);
            Long b2 = handler.getTransactionBranchId(gId, appName);
            List<String> list = new ArrayList<>();
            list.add("data-1");
            list.add("data-2");
            handler.lockData(appName, gId, b2, list);
            handler.doCommit(gId, b2, appName);
            try {
                holdOn(100);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            semaphore.release();
        }).start();
        holdOn(100);
        handler.doCommit(txGroupId, branchId, appName);
        semaphore.acquire();
    }

    private void deadLockTest(TestTransactionManager manager, Method method, TransactionHandler handler, String appName) throws InterruptedException {
        DistributedTransactionContext.clear();
        manager.beginTransaction(method);
        Long txGroupId = manager.getCurrentTransactionObject().getTxGroupId();
        Long branchId = handler.getTransactionBranchId(txGroupId, appName);
        List<String> locks = new ArrayList<>();
        locks.add("data-10");
        locks.add("data-20");
        handler.lockData(appName, txGroupId, branchId, locks);

        branchId = handler.getTransactionBranchId(txGroupId, appName);
        locks = new ArrayList<>();
        locks.add("data-20");
        locks.add("data-30");
        try {
            handler.lockData(appName, txGroupId, branchId, locks);
            throw new RuntimeException("impossible");
        } catch (Exception e) {
            TestCase.assertEquals(DeadLockException.class, e.getClass());
            logger.error(e.getMessage());
            handler.doRollback(txGroupId, appName);
        }

    }

    private void emptyTransactionTest(TransactionHandler handler, String appName) {
        Long txGroupId;// 空回滚
        txGroupId = handler.getTransactionGroupId(appName);
        handler.doRollback(txGroupId, appName);
        // 回滚不存在的事务
        try {
            handler.doRollback(-1L, appName);
            throw new RuntimeException("不可能走到这一步");
        } catch (Exception e) {
            TestCase.assertEquals(DistributedTransactionException.class, e.getClass());
        }
        // 空提交
        txGroupId = handler.getTransactionGroupId(appName);
        handler.doCommit(txGroupId, null, appName);
        // 提交不存在的事务
        try {
            handler.doCommit(-1L, null, appName);
            throw new RuntimeException("不可能走到这一步");
        } catch (Exception e) {
            TestCase.assertEquals(DistributedTransactionException.class, e.getClass());
        }
    }

    private void normalRollbackAndCommitTest(TransactionHandler handler, String appName) {
        // 正常多分支提交
        Long txGroupId = handler.getTransactionGroupId(appName);
        Long branchId = handler.getTransactionBranchId(txGroupId, appName);
        handler.doBranchCommit(txGroupId, branchId, appName);
        branchId = handler.getTransactionBranchId(txGroupId, appName);
        handler.doBranchCommit(txGroupId, branchId, appName);
        handler.doCommit(txGroupId, null, appName);

        // 正常多分支回滚
        txGroupId = handler.getTransactionGroupId(appName);
        branchId = handler.getTransactionBranchId(txGroupId, appName);
        handler.doBranchCommit(txGroupId, branchId, appName);
        // 开启新分支不提交
        handler.getTransactionBranchId(txGroupId, appName);
        // 直接回滚，模拟第二分支异常了
        handler.doRollback(txGroupId, appName);
    }

    private void mockDeadContext(PooledJedisClient jedisClient, TestTransactionManager manager, long base) {
        jedisClient.zadd(RedisKeyNames.DTX_CONTEXT_LIST.name(),
                System.currentTimeMillis() - 40L * 60 * 1000,
                getGroupIdKey(-10L + base));

        jedisClient.zadd(RedisKeyNames.DTX_CONTEXT_LIST.name(),
                System.currentTimeMillis() - 50L * 60 * 1000,
                getGroupIdKey(-11L + base));
        jedisClient.hset(getGroupIdKey(-11L + base), RedisKeyNames.GROUP_INFO.name(),
                manager.getApplicationName() + "|" + TxStatus.OPEN + "|" + (-11 + base));
        jedisClient.hset(getGroupIdKey(-11L + base), getBranchInfoKey(-31 + base),
                manager.getApplicationName() + "|" + TxStatus.COMMITTED + "|" + (-31 + base));
        redisTransactionHandler.getSweeper().wakeUp();
    }

    private DtxServerWrapper createServer(int port) throws IOException {
        DtxServerEventHandler serverEventHandler = new DtxServerEventHandler();
        serverEventHandler.setTransactionHandler(redisTransactionHandler);
        serverEventHandler.init();
        return new DtxServerWrapper(port, serverEventHandler);
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
            return Arrays.asList(
                    new Node("127.0.0.1", 12345),
                    new Node("127.0.0.1", 12346),
                    new Node("127.0.0.1", 12347),
                    new Node("127.0.0.1", 12348),
                    new Node("127.0.0.1", 12349)
            );
        }
    }

    private String getGroupIdKey(Long txGroupId) {
        return RedisKeyNames.DTX_GROUP_ID + "_" + txGroupId.toString();
    }

    private String getBranchInfoKey(Long txBranchId) {
        return RedisKeyNames.BRANCH_INFO.name() + txBranchId.toString();
    }




}

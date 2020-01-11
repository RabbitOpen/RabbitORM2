package rabbit.open.dtx.server.test;

import junit.framework.TestCase;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.algorithm.elect.data.NodeRole;
import rabbit.open.dtx.common.context.DistributedTransactionContext;
import rabbit.open.dtx.common.nio.client.AbstractMessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.pub.inter.TransactionHandler;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;
import rabbit.open.dtx.server.DtxServerClusterWrapper;
import rabbit.open.dtx.server.RedisKeyNames;
import rabbit.open.dtx.server.handler.RedisTransactionHandler;
import rabbit.open.dtx.server.jedis.PooledJedisClient;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 基于redis的事务管理器
 * @author xiaoqianbin
 * @date 2020/1/2
 **/
@RunWith(JUnit4.class)
public class ClusterServerTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    // 集群节点个数
    private int nodeNum = 3;

    private JedisPool getPool() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(3);
        JedisPool pool = new JedisPool(poolConfig, "localhost", 6379);
        return pool;
    }

    @Test
    public void clusterServerTest() throws InterruptedException, IOException, NoSuchMethodException {
        PooledJedisClient jedisClient = new PooledJedisClient(getPool());
        List<DtxServerClusterWrapper> serverWrappers = new ArrayList<>();
        Semaphore semaphore = new Semaphore(0);
        for (int i = 0; i < nodeNum; i++) {
            holdOn(50);
            ServerThread thread = new ServerThread(16345 + i) {
                @Override
                public void run() {
                    try {
                        serverWrappers.add(createServer(port, new PooledJedisClient(getPool())));
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    semaphore.release();
                }
            };
            thread.start();
        }
        semaphore.acquire(nodeNum);
        holdOn(1000);

        Long count = jedisClient.zcount(RedisKeyNames.DTX_CONTEXT_LIST.name(), 0, Long.MAX_VALUE);
        TestTransactionManager manager = new TestTransactionManager();
        manager.init();

        // 避免被其他单元测试干扰
        DistributedTransactionContext.clear();
        Method method = HelloService.class.getDeclaredMethod("hello1");
        manager.beginTransaction(method);
        TransactionHandler handler = manager.getTransactionHandler();
        Long txGroupId = manager.getCurrentTransactionObject().getTxGroupId();
        String appName = manager.getApplicationName();
        Long branchId = handler.getTransactionBranchId(txGroupId, appName);
        handler.doBranchCommit(txGroupId, branchId, appName);
        handler.doCommit(txGroupId, branchId, appName);
        manager.destroy();

        // 避免干扰其他单元测试
        DistributedTransactionContext.clear();
        holdOn(1000);
        DtxServerClusterWrapper closeServer = serverWrappers.remove(0);
        closeServer.close();
        holdOn(1000);
        ServerThread thread = new ServerThread(closeServer.getPort()) {
            @Override
            public void run() {
                try {
                    serverWrappers.add(createServer(port, new PooledJedisClient(getPool())));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        };
        thread.start();
        holdOn(1000);
        TestCase.assertEquals(count, jedisClient.zcount(RedisKeyNames.DTX_CONTEXT_LIST.name(), 0, Long.MAX_VALUE));
        jedisClient.close();
        holdOn(2000);
        closeLeader(serverWrappers);
        holdOn(2000);
        for (DtxServerClusterWrapper server : serverWrappers) {
        	server.close();
        }
    }

	protected void closeLeader(List<DtxServerClusterWrapper> serverWrappers) {
		for (DtxServerClusterWrapper server : serverWrappers) {
            if (server.getArbiter().getNodeRole() == NodeRole.LEADER) {
            	server.close();
            	serverWrappers.remove(server);
            	break;
            }
        }
	}

    private DtxServerClusterWrapper createServer(int port, PooledJedisClient jedisClient) throws IOException {
        RedisTransactionHandler redisTransactionHandler = new RedisTransactionHandler();
        redisTransactionHandler.setJedisClient(jedisClient);
        DtxServerEventHandler serverEventHandler = new DtxServerEventHandler();
        serverEventHandler.setTransactionHandler(redisTransactionHandler);
        serverEventHandler.init();
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < nodeNum; i++) {
            nodes.add(new Node(InetAddress.getLocalHost().getHostAddress(), 16345 + i));
        }
        return new DtxServerClusterWrapper(port, serverEventHandler, nodeNum, nodes);
    }

    private void holdOn(long milliSeconds) throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        if (semaphore.tryAcquire(milliSeconds, TimeUnit.MILLISECONDS)) {
            // 等100ms，保证response已经回来了
            throw new RuntimeException("见鬼了");
        }
    }

    public class ServerThread extends Thread {

        protected int port;

        public ServerThread(int port) {
            this.port = port;
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
            return "cluster-test-app";
        }

        @Override
        public List<Node> getServerNodes() {
            List<Node> list = new ArrayList<>();
            for (int i = 0; i < 1; i++) {
                try {
                    list.add(new Node(InetAddress.getLocalHost().getHostAddress(), 16345 + i));
                } catch (UnknownHostException e) {
                    logger.error(e.getMessage());
                }
            }
            return list;
        }
    }
 }

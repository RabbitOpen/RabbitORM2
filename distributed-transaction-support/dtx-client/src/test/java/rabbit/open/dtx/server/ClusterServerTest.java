package rabbit.open.dtx.server;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.server.ClusterDtxServerWrapper;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;
import rabbit.open.dtx.common.nio.server.PooledJedisClient;
import rabbit.open.dtx.common.nio.server.RedisTransactionHandler;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.net.InetAddress;
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
    private int count = 3;

    private JedisPool getPool() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(3);
        JedisPool pool = new JedisPool(poolConfig, "localhost", 6379);
        return pool;
    }

    @Test
    public void clusterServerTest() throws IOException, InterruptedException {
        PooledJedisClient jedisClient = new PooledJedisClient(getPool());
        List<ClusterDtxServerWrapper> serverWrappers = new ArrayList<>();
        Semaphore semaphore = new Semaphore(0);
        for (int i = 0; i < count; i++) {
            ServerThread thread = new ServerThread(16345 + i) {
                @Override
                public void run() {
                    try {
                        serverWrappers.add(createServer(port, jedisClient));
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    semaphore.release();
                }
            };
            thread.start();
        }
        semaphore.acquire(count);
        holdOn(1000);

        for (ClusterDtxServerWrapper serverWrapper : serverWrappers) {
            serverWrapper.close();
        }
    }

    private ClusterDtxServerWrapper createServer(int port, PooledJedisClient jedisClient) throws IOException {
        RedisTransactionHandler redisTransactionHandler = new RedisTransactionHandler();
        redisTransactionHandler.setJedisClient(jedisClient);
        DtxServerEventHandler serverEventHandler = new DtxServerEventHandler();
        serverEventHandler.setTransactionHandler(redisTransactionHandler);
        serverEventHandler.init();
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            nodes.add(new Node(InetAddress.getLocalHost().getHostAddress(), 16345 + i));
        }
        return new ClusterDtxServerWrapper(port, serverEventHandler, count, nodes);
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
 }

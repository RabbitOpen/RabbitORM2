package rabbit.open.dtx.rpc.test;

import junit.framework.TestCase;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import rabbit.open.dtx.common.exception.DistributedTransactionException;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;
import rabbit.open.dtx.rpc.test.client.MyService;
import rabbit.open.dtx.rpc.test.client.RollbackException;
import rabbit.open.dtx.rpc.test.server.NestedZookeeperServer;
import rabbit.open.dtx.rpc.test.server.User;
import rabbit.open.dtx.server.DtxServerWrapper;
import rabbit.open.dtx.server.handler.RedisTransactionHandler;
import rabbit.open.dtx.server.jedis.PooledJedisClient;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaoqianbin
 * @date 2020/1/16
 **/
@RunWith(JUnit4.class)
public class DubboTxTest {

    private NestedZookeeperServer zookeeperServer;
    private ClassPathXmlApplicationContext consumerContext;
    private ClassPathXmlApplicationContext providerContext;
    private Semaphore consumed = new Semaphore(0);
    private Semaphore over = new Semaphore(0);

    @Test
    public void invokeTest() throws InterruptedException {
        startProvider();
        new Semaphore(0).tryAcquire(1, TimeUnit.SECONDS);
        startConsumer();
        over.acquire();
//        zookeeperServer.shutdown();
    }

    private void startConsumer() {
        new Thread(() -> {
            consumerContext = new ClassPathXmlApplicationContext("classpath*:consumer.xml");
            MyService myService = (MyService) consumerContext.getBean("myService");
            Long id = myService.clientAppAddUser("zhangsan", 10);
            User user = myService.getUserById(id);
            TestCase.assertNotNull(user);
            TestCase.assertTrue(10 == user.getAge());
            TestCase.assertEquals(user.getName(), "zhangsan");
            try {
                myService.addUserAndRollback("list", 100);
                throw new RuntimeException("impossible");
            } catch (DistributedTransactionException e) {
                RollbackException r = (RollbackException) e.getCause();
                TestCase.assertNotNull(r.getId());
                TestCase.assertNull(myService.getUserById(r.getId()));
            }
            consumerContext.close();
            consumed.release();
        }).start();
    }

    private void startProvider() {
        Thread providerThread = new Thread(() -> {
            try {
                // 启动server
                PooledJedisClient jedisClient = new PooledJedisClient(getPool());
                RedisTransactionHandler redisTransactionHandler = new RedisTransactionHandler();
                redisTransactionHandler.setJedisClient(jedisClient);
                DtxServerEventHandler serverEventHandler = new DtxServerEventHandler();
                serverEventHandler.setTransactionHandler(redisTransactionHandler);
                serverEventHandler.init();
                DtxServerWrapper serverWrapper = new DtxServerWrapper("localhost", 10010, serverEventHandler);
                // 启动ZK
                zookeeperServer = new NestedZookeeperServer();
                zookeeperServer.start();

                providerContext = new ClassPathXmlApplicationContext("classpath*:provider.xml");
                // 等待消费
                consumed.acquire();
                // 释放资源
                providerContext.close();
                serverWrapper.close();
                redisTransactionHandler.destroy();
                over.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        providerThread.start();
    }

    private JedisPool getPool() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(3);
        JedisPool pool = new JedisPool(poolConfig, "localhost", 6379);
        return pool;
    }
}

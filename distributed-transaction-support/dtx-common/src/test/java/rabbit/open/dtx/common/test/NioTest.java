package rabbit.open.dtx.common.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.client.AgentMonitor;
import rabbit.open.dtx.common.nio.client.FutureResult;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.protocol.KeepAlive;
import rabbit.open.dtx.common.nio.server.DtxServer;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;
import rabbit.open.dtx.common.nio.server.ServerAgentMonitor;
import rabbit.open.dtx.common.nio.server.handler.ApplicationDataHandler;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * nio相关测试
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@RunWith(JUnit4.class)
public class NioTest {

    private static final Logger logger = LoggerFactory.getLogger(NioTest.class);

    /**
     * 100W次通信测试
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    @Test
    public void nio1000kTest() throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        int port = 10000;
        DtxServerEventHandler handler = new DtxServerEventHandler();
        handler.init();
        DtxServer server = new DtxServer(port, handler);
        server.start();
        TestTransactionManager manager = new TestTransactionManager();
        DtxChannelAgentPool arp = new DtxChannelAgentPool(manager);
        int count = 100;
        CountDownLatch cdl = new CountDownLatch(count);
        long start = System.currentTimeMillis();

        // 调整 server monitor的检测间隔
//        adjustServerMonitorParam(server);
//        adjustClientMonitor(arp);

        for (int c = 0; c < count; c++) {
            new Thread(() -> {
                for (int i = 0; i < 10000; i++) {
                    try {
                        ChannelAgent agent = arp.getResource();
                        FutureResult result = agent.send(new KeepAlive());
                        agent.release();
                        result.getData();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                cdl.countDown();
            }).start();
        }
        cdl.await();
        logger.info("cost: {}", (System.currentTimeMillis() - start));
        TestCase.assertEquals(ApplicationDataHandler.getAgents(manager.getApplicationName()).size(), arp.getResourceCount());
        Semaphore s = new Semaphore(0);
        if (s.tryAcquire(50, TimeUnit.MILLISECONDS)) {
            logger.info("不可能走到这里来， 等待50ms，让client minitor发送心跳包");
        }
        StringBuilder longStr = new StringBuilder();
        for (int i = 0 ; i < 1024; i++) {
            longStr.append("hello world");
        }
        ChannelAgent agent = arp.getResource();
        FutureResult result = agent.send(longStr.toString());
        agent.release();
        Object data = result.getData();
        TestCase.assertTrue(data instanceof Exception);
        logger.info("{} ", data.toString());
        arp.gracefullyShutdown();
        server.shutdown();
        TestCase.assertEquals(ChannelAgent.getLeftMessages(), 0);
        TestCase.assertEquals(0, ApplicationDataHandler.getAgents(manager.getApplicationName()).size());
    }

    private void adjustClientMonitor(DtxChannelAgentPool arp) throws NoSuchFieldException, IllegalAccessException {
        Field agentMonitor = DtxChannelAgentPool.class.getDeclaredField("monitor");
        agentMonitor.setAccessible(true);
        AgentMonitor monitor = (AgentMonitor) agentMonitor.get(arp);
        Field idleMilliSecondsThreshold = AgentMonitor.class.getDeclaredField("idleMilliSecondsThreshold");
        idleMilliSecondsThreshold.setAccessible(true);
        idleMilliSecondsThreshold.set(monitor, 1);
        monitor.wakeup();
    }

    private void adjustServerMonitorParam(DtxServer server) throws NoSuchFieldException, IllegalAccessException {
        Field agentMonitor = DtxServer.class.getDeclaredField("serverAgentMonitor");
        agentMonitor.setAccessible(true);
        ServerAgentMonitor monitor = (ServerAgentMonitor) agentMonitor.get(server);
        Field checkIntervalSeconds = ServerAgentMonitor.class.getDeclaredField("checkIntervalSeconds");
        checkIntervalSeconds.setAccessible(true);
        checkIntervalSeconds.set(monitor, 1);
        monitor.wakeup();
    }


}

package rabbit.open.dtx.common.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.client.FutureResult;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.protocol.KeepAlive;
import rabbit.open.dtx.common.nio.server.DtxServer;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;
import rabbit.open.dtx.common.nio.server.handler.ApplicationDataHandler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

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
    public void nio1000kTest() throws IOException, InterruptedException {
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


}

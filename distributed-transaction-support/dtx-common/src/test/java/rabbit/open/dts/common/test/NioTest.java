package rabbit.open.dts.common.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dts.common.rpc.nio.client.AbstractResourcePool;
import rabbit.open.dts.common.rpc.nio.client.DtxClient;
import rabbit.open.dts.common.rpc.nio.client.FutureResult;
import rabbit.open.dts.common.rpc.nio.client.Node;
import rabbit.open.dts.common.rpc.nio.client.ext.DtxResourcePool;
import rabbit.open.dts.common.rpc.nio.pub.DataType;
import rabbit.open.dts.common.rpc.nio.pub.KeepAlive;
import rabbit.open.dts.common.rpc.nio.server.DtxServer;

import java.io.IOException;
import java.util.Arrays;
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
        ServerNetEventHandler handler = new ServerNetEventHandler();
        DtxServer server = new DtxServer(port, handler);
        server.start();
        AbstractResourcePool<DtxClient> arp = new DtxResourcePool(5, Arrays.asList(new Node("localhost", port)));
        int count = 100;
        CountDownLatch cdl = new CountDownLatch(count);
        long start = System.currentTimeMillis();
        for (int c = 0; c < count; c++) {
            new Thread(() -> {
                for (int i = 0; i < 10000; i++) {
                    try {
                        DtxClient dtxClient = arp.getResource(50);
                        FutureResult result = dtxClient.send(new KeepAlive());
                        dtxClient.release();
                        result.getData();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                cdl.countDown();
            }).start();
        }
        cdl.await();
        arp.gracefullyShutdown();
        logger.info("cost: {}", (System.currentTimeMillis() - start));
        server.shutdown();
        TestCase.assertEquals(DtxClient.getLeftMessages(), 0);
    }
}

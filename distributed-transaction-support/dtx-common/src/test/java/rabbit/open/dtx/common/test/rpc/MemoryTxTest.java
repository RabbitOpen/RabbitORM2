package rabbit.open.dtx.common.test.rpc;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;
import rabbit.open.dtx.common.nio.server.DtxServerWrapper;
import rabbit.open.dtx.common.nio.server.MemoryBasedTransactionHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:support.xml"})
public class MemoryTxTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    MemoryTransactionManger rtm;

    static long groupId;

    @Test
    public void rpcTest() throws IOException, InterruptedException {
        DtxServerEventHandler handler = new DtxServerEventHandler();
        handler.init();
        handler.setTransactionHandler(new MemoryBasedTransactionHandler());
        DtxServerWrapper dtxServerWrapper = new DtxServerWrapper(10021, handler);
        int count = 1;
        int loop = 10;
        CountDownLatch cdl = new CountDownLatch(count);
        long start = System.currentTimeMillis();
        TransactionHandler clientTransactionHandler = rtm.getTransactionHandler();
        for (int index = 0; index < count; index++) {
            new Thread(() -> {
                for (int i = 0; i < loop; i++) {
                    groupId = clientTransactionHandler.getTransactionGroupId(rtm.getApplicationName());
                }
                cdl.countDown();
            }).start();
        }
        cdl.await();
        logger.info("cost {}", System.currentTimeMillis() - start);
        TestCase.assertEquals(groupId, count * loop - 1);

        String applicationName = "cx";
        Long groupId = clientTransactionHandler.getTransactionGroupId(applicationName);
        clientTransactionHandler.doBranchCommit(groupId, 2L, "cx");
        clientTransactionHandler.doCommit(groupId, null, "cx");
        clientTransactionHandler.doRollback(100L);
        dtxServerWrapper.close();
        handler.onServerClosed();
    }

}

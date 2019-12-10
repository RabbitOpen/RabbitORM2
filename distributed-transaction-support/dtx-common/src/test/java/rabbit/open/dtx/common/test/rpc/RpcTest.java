package rabbit.open.dtx.common.test.rpc;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.dtx.common.nio.exception.RpcException;
import rabbit.open.dtx.common.nio.exception.TimeoutException;
import rabbit.open.dtx.common.test.ServerNetEventHandler;
import rabbit.open.dtx.common.test.enhance.FirstEnhancer;
import rabbit.open.dtx.common.test.enhance.HelloService;
import rabbit.open.dtx.common.test.enhance.LastEnhancer;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:support.xml"})
public class RpcTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TestServerWrapper serverWrapper;

    @Autowired
    DtxTransactionClient client;

    @Autowired
    private HelloService helloService;

    static long groupId;

    @Test
    public void rpcTest() throws IOException, InterruptedException {
        serverWrapper.start(10086, new ServerNetEventHandler());
        int count = 100;
        int loop = 10000;
        CountDownLatch cdl = new CountDownLatch(count);
        long start = System.currentTimeMillis();
        for (int index = 0; index < count; index++) {
            new Thread(() -> {
                for (int i = 0; i < loop; i++) {
                    groupId = client.getTransactionGroupId();
                }
                cdl.countDown();
            }).start();
        }
        cdl.await();
        logger.info("cost {}", System.currentTimeMillis() - start);
        TestCase.assertEquals(groupId, count * loop - 1);

        client.doBranchCommit(1L, 2L, "rpcTest");
        client.doCommit(11L, 3L, "rpcTest");

        //超时
        try {
            client.doRollback(100L);
            throw new RpcException("");
        } catch (Exception e) {
            logger.warn(e.getMessage());
            TestCase.assertEquals(e.getClass(), TimeoutException.class);
        }
    }

    @Test
    public void handlerTest() {
        String name = "san";
        String result = helloService.sayHello(name);
        logger.info(result);
        TestCase.assertEquals(FirstEnhancer.class.getSimpleName() + LastEnhancer.class.getSimpleName()
                + "hello" + name, result);
    }
}

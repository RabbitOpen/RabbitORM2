package rabbit.open.dtx.common.test.rpc;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.dtx.common.nio.exception.RpcException;
import rabbit.open.dtx.common.nio.exception.TimeoutException;
import rabbit.open.dtx.common.nio.server.DtxServer;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;
import rabbit.open.dtx.common.test.enhance.FirstEnhancer;
import rabbit.open.dtx.common.test.enhance.HelloService;
import rabbit.open.dtx.common.test.enhance.HerService;
import rabbit.open.dtx.common.test.enhance.LastEnhancer;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:support.xml"})
public class RpcTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private TestServerWrapper serverWrapper;

    @Resource
    RpcTransactionManager rtm;

    @Resource
    private HelloService helloService;

    @Resource
    private HerService herService;

    static long groupId;

    @Test
    public void rpcTest() throws IOException, InterruptedException, NoSuchFieldException {
        DtxServerEventHandler handler = new DtxServerEventHandler();
        handler.init();
        handler.setTransactionHandler(new MyTransactionService());
        serverWrapper.start(10086, handler);
        rtm.manualInit();
        int count = 100;
        int loop = 100;
        CountDownLatch cdl = new CountDownLatch(count);

        Field errCount = DtxServer.class.getDeclaredField("errCount");
        new Thread(() -> {
            Semaphore s = new Semaphore(0);
            for (int i = 0; i < 10; i++) {
                try {
                    s.tryAcquire(new Random().nextInt(500), TimeUnit.MILLISECONDS);
                    errCount.setAccessible(true);
                    errCount.set(serverWrapper.getServer(), 110);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }).start();
        long start = System.currentTimeMillis();
        for (int index = 0; index < count; index++) {
            new Thread(() -> {
                for (int i = 0; i < loop; i++) {
                    groupId = rtm.getTransactionHandler().getTransactionGroupId(rtm.getApplicationName());
                }
                cdl.countDown();
            }).start();
        }
        cdl.await();
        logger.info("cost {}", System.currentTimeMillis() - start);
        TestCase.assertEquals(groupId, count * loop - 1);

        rtm.getTransactionHandler().doBranchCommit(1L, 2L, "rpcTest");
        rtm.getTransactionHandler().doCommit(11L, 3L, "rpcTest");

        //超时
        try {
            rtm.getTransactionHandler().doRollback(100L, "rpcTest");
            throw new RpcException("");
        } catch (TimeoutException e) {
            logger.warn(e.getMessage());
            TestCase.assertEquals(e.getTimeoutSeconds(), rtm.getRpcTimeoutSeconds());
        }
        rtm.destroy();
    }

    @Test
    public void handlerTest() {
        String name = "san";
        String result = helloService.sayHello(name);
        logger.info(result);
        TestCase.assertEquals(FirstEnhancer.class.getSimpleName() + LastEnhancer.class.getSimpleName()
                + "hello" + name, result);

        // 自定义增强和aop并不冲突
        TestCase.assertEquals(herService.sayHello(name), result);
    }
}

package rabbit.open.dtx.common.test.rpc;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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


    @Test
    public void rpcTest() throws IOException, InterruptedException {
        serverWrapper.start(10086, new ServerNetEventHandler());
        int count = 100;
        CountDownLatch cdl = new CountDownLatch(count);
        long start = System.currentTimeMillis();
        for (int index = 0; index < count; index++) {
            new Thread(() -> {
                for (int i = 0; i < 10000; i++) {
                    Long groupId = client.getTransactionGroupId();
                };
                cdl.countDown();
            }).start();
        }
        cdl.await();
        logger.info("cost {}", System.currentTimeMillis() - start);
    }

    @Test
    public void handlerTest() {
        String name = " zhangsan ";
        String result = helloService.sayHello(name);
        logger.info(result);
        TestCase.assertEquals(FirstEnhancer.class.getSimpleName() + LastEnhancer.class.getSimpleName()
                + "hello" + name, result);
    }
}

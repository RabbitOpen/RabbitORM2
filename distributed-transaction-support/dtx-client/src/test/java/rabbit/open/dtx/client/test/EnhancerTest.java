package rabbit.open.dtx.client.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rabbit.open.dtx.client.test.service.MyDistributedTransactionEnhancer;
import rabbit.open.dtx.client.test.service.ProductService;
import rabbit.open.dtx.client.test.service.SimpleTransactionManager;
import rabbit.open.dtx.common.exception.DistributedTransactionException;
import rabbit.open.dtx.common.nio.pub.inter.TransactionHandler;

import java.util.List;
import java.util.concurrent.CountDownLatch;


/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:support.xml"})
public class EnhancerTest {

    @Autowired
    private ProductService productService;

    @Autowired
    MyDistributedTransactionEnhancer enhancer;

    @Autowired
    SimpleTransactionManager stm;

    boolean rollback = false;

    @Test
    public void invokeTest() throws InterruptedException {
        int count = 5;
        CountDownLatch cdl = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                productService.async();
                cdl.countDown();
            }).start();
        }
        cdl.await();
        stm.setTransactionHandler(new TransactionHandler() {
            @Override
            public void doBranchCommit(Long txGroupId, Long txBranchId, String applicationName) {

            }

            @Override
            public void doCommit(Long txGroupId, Long txBranchId, String applicationName) {

            }

            @Override
            public void confirmBranchRollback(String applicationName, Long txGroupId, Long txBranchId) {

            }

            @Override
            public void doRollback(Long txGroupId, String applicationName) {
                rollback = true;
            }

            @Override
            public Long getTransactionBranchId(Long txGroupId, String applicationName) {
                return stm.getIdGenerator().getAndAdd(1L);
            }

            @Override
            public Long getTransactionGroupId(String applicationName) {
                return stm.getIdGenerator().getAndAdd(1L);
            }

            @Override
            public void lockData(String applicationName, Long txGroupId, Long txBranchId, List<String> locks) {

            }
        });
        try {
            productService.asyncException();
            throw new RuntimeException("impossible");
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), DistributedTransactionException.class);
        }
        TestCase.assertTrue(rollback);
        try {
            enhancer.setNestedOnly(false);
            productService.nested();
            productService.nestedException();
            throw new RuntimeException("impossible");
        } catch (Exception e) {
            TestCase.assertEquals(e.getClass(), DistributedTransactionException.class);
        } finally {
            TestCase.assertTrue(enhancer.isNestedOnly());
        }
    }

}

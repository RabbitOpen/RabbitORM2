package rabbit.open.dtx.common.test.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.spring.anno.DtxService;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试dtxService
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
@DtxService
public class MyTransactionService implements TransactionHandler {

    private Logger logger = LoggerFactory.getLogger(getClass());

    AtomicLong atomicLong = new AtomicLong(0);

    private Semaphore semaphore = new Semaphore(0);

    @Override
    public Long getTransactionGroupId() {
        return atomicLong.getAndAdd(1);
    }

    @Override
    public void doCommit(Long txGroupId, Long txBranchId, String applicationName) {
        logger.info("received txGroupId:{}, txBranchId:{}, app:{}", txGroupId, txBranchId, applicationName);
    }

    @Override
    public void doRollback(Long txGroupId) {
        try {
            if (semaphore.tryAcquire(4, TimeUnit.SECONDS)){
                logger.info("timeout");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}

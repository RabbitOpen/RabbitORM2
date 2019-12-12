package rabbit.open.dtx.common.test.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.server.TxStatus;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerTransactionHandler;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试dtxService
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
public class MyTransactionService extends AbstractServerTransactionHandler {

    private Logger logger = LoggerFactory.getLogger(getClass());

    AtomicLong atomicLong = new AtomicLong(0);

    private Semaphore semaphore = new Semaphore(0);

    @Override
    public Long getTransactionGroupId(String applicationName) {
        return atomicLong.getAndAdd(1);
    }

    @Override
    protected void doCommitByGroupId(Long txGroupId) {

    }

    @Override
    protected void doRollbackByGroupId(Long txGroupId) {

    }

    @Override
    protected Long getNextGlobalId() {
        return null;
    }

    @Override
    protected void persistGroupId(Long txGroupId, String applicationName, TxStatus txStatus) {

    }

    @Override
    protected void persistGroupId(Long txGroupId, TxStatus txStatus) {

    }

    @Override
    protected void persistBranchInfo(Long txGroupId, Long txBranchId, String applicationName, TxStatus txStatus) {

    }

    @Override
    public void doCommit(Long txGroupId, Long txBranchId, String applicationName) {
        logger.info("received txGroupId:{}, txBranchId:{}, app:{}", txGroupId, txBranchId, applicationName);
    }

    @Override
    public void doRollback(Long txGroupId, String applicationName) {
        try {
            if (semaphore.tryAcquire(4, TimeUnit.SECONDS)){
                logger.info("timeout");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}

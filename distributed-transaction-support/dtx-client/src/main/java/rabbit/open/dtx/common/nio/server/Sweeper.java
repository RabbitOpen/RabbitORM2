package rabbit.open.dtx.common.nio.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.pub.CallHelper;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 清道夫：负责清理 RedisTransactionHandler中死掉的context
 * @author xiaoqianbin
 * @date 2020/1/2
 **/
public class Sweeper extends Thread {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private boolean run = true;

    private Semaphore semaphore = new Semaphore(0);

    private RedisTransactionHandler redisTransactionHandler;

    public Sweeper(RedisTransactionHandler redisTransactionHandler) {
        this.redisTransactionHandler = redisTransactionHandler;
    }

    @Override
    public void run() {
        while (run) {
            try {
                redisTransactionHandler.clearDeadContext();
                if (semaphore.tryAcquire(30, TimeUnit.MINUTES)) {
                    continue;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void wakeUp() {
        semaphore.release();
    }

    public void shutdown() {
        run = false;
        wakeUp();
        CallHelper.ignoreExceptionCall(this::join);
    }
}

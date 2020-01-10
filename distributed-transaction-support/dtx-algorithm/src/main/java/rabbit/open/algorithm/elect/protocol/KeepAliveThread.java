package rabbit.open.algorithm.elect.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.algorithm.elect.data.HelloKitty;
import rabbit.open.algorithm.elect.data.NodeRole;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 心跳线程
 * @author xiaoqianbin
 * @date 2019/12/30
 **/
public class KeepAliveThread extends Thread {

    private Logger logger = LoggerFactory.getLogger(getName());

    private ElectionArbiter arbiter;

    private Semaphore semaphore = new Semaphore(0);

    public KeepAliveThread(ElectionArbiter arbiter) {
        super(KeepAliveThread.class.getSimpleName());
        this.arbiter = arbiter;
    }

    @Override
    public void run() {
        while (true) {
            if (NodeRole.LEADER == arbiter.getNodeRole()) {
                arbiter.postman.delivery(new HelloKitty(arbiter.getNodeId()));
            }
            try {
                if (semaphore.tryAcquire(2, TimeUnit.SECONDS)) {
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void shutdown() throws InterruptedException {
        logger.info("{} is closing....", getName());
        semaphore.release();
        join();
        logger.info("{} is closed", getName());
    }
}

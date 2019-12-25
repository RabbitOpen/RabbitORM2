package rabbit.open.dtx.common.nio.server;

import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;

import javax.annotation.PostConstruct;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式事务服务端网络事件处理器
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
public class DtxServerEventHandler extends AbstractServerEventHandler {

    // 核心线程数
    private int bossCoreSize = 5;

    // 最大并发数
    private int maxBossConcurrence = 10;

    private int maxBossQueueSize = 1000;

    // 核心线程数
    private int workerCoreSize = 10;

    // 最大并发数
    private int maxWorkerConcurrence = 30;

    private int maxWorkerQueueSize = 1000;

    // 线程池
    private ThreadPoolExecutor bossPool;

    private ThreadPoolExecutor workerPool;

    @PostConstruct
    public void init() {
        bossPool = new ThreadPoolExecutor(bossCoreSize, maxBossConcurrence, 5, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(maxBossQueueSize), new DtxThreadFactory("boss-pool"), (r, executor) -> r.run());

        workerPool = new ThreadPoolExecutor(workerCoreSize, maxWorkerConcurrence, 5, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(maxWorkerQueueSize), new DtxThreadFactory("worker-pool"), (r, executor) -> r.run());
    }

    @Override
    protected void processData(ProtocolData protocolData, ChannelAgent agent) {
        workerPool.submit(() -> {
            try {
                cacheAgent(agent);
                super.processData(protocolData, agent);
            } finally {
                clearAgent();
            }
        });
    }

    /**
     * 数据读取任务交由线程池并发处理
     * @param	task
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    @Override
    protected void executeReadTask(Runnable task) {
        bossPool.submit(task);
    }

    @Override
    public void onServerClosed() {
        logger.info("begin to close boss pool");
        bossPool.shutdown();
        logger.info("boss pool closed");
        logger.info("begin to close worker pool");
        workerPool.shutdown();
        logger.info("worker pool closed");
    }

    public void setBossCoreSize(int bossCoreSize) {
        this.bossCoreSize = bossCoreSize;
    }

    public void setMaxBossConcurrence(int maxBossConcurrence) {
        this.maxBossConcurrence = maxBossConcurrence;
    }

    public void setMaxBossQueueSize(int maxBossQueueSize) {
        this.maxBossQueueSize = maxBossQueueSize;
    }

    /**
     * 自定义线程池工厂
     * @author  xiaoqianbin
     * @date    2019/12/19
     **/
    private class DtxThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DtxThreadFactory(String prefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = prefix + "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}

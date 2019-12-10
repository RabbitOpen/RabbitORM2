package rabbit.open.dtx.common.nio.server.ext;

import rabbit.open.dtx.common.nio.pub.ChannelAgent;

import javax.annotation.PostConstruct;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 分布式事务服务端网络事件处理器
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
public final class DtxServerEventHandler extends AbstractServerEventHandler {

    // 核心线程数
    private int coreSize = 10;

    // 最大并发数
    private int maxConcurrence = 30;

    // 线程池
    private ThreadPoolExecutor tpe;

    @PostConstruct
    public void init() {
        tpe = new ThreadPoolExecutor(coreSize, maxConcurrence, 5, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(1000), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                r.run();
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
        tpe.submit(task);
    }

    @Override
    public void onServerClosed() {
        tpe.shutdown();
    }
}

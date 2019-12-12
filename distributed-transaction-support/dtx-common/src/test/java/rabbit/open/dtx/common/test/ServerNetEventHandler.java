package rabbit.open.dtx.common.test;

import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 服务器网络事件处理器
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
public class ServerNetEventHandler extends AbstractServerEventHandler {

    ThreadPoolExecutor bossPool = new ThreadPoolExecutor(3, 5, 5,
            TimeUnit.MINUTES, new ArrayBlockingQueue<>(10), (r, executor) -> r.run());

    @Override
    protected void executeReadTask(Runnable task) {
        bossPool.submit(task);
    }

    @Override
    public void onServerClosed() {
        bossPool.shutdown();
    }
}

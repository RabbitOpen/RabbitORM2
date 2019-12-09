package rabbit.open.dtx.common.test;

import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 服务器网络事件处理器
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
public class ServerNetEventHandler extends AbstractServerEventHandler {

    ThreadPoolExecutor tpe = new ThreadPoolExecutor(10, 20, 5, TimeUnit.MINUTES, new ArrayBlockingQueue<>(1000), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            r.run();
        }
    });

    @Override
    public void onConnected(ChannelAgent agent) {
        logger.info("{} connected ", agent.getRemote());
    }

    @Override
    protected void executeReadTask(Runnable task) {
        tpe.submit(task);
    }

    @Override
    public void onServerClosed() {
        tpe.shutdown();
    }
}

package rabbit.open.dtx.common.nio.server;

import rabbit.open.dtx.common.nio.client.AgentMonitor;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.NioSelector;

import java.nio.channels.SelectionKey;
import java.util.concurrent.TimeUnit;

/**
 * 服务端监控器
 * @author xiaoqianbin
 * @date 2019/12/18
 **/
public class ServerAgentMonitor extends AgentMonitor {

    private NioSelector selector;

    private int checkIntervalSeconds = 30;

    public ServerAgentMonitor(String name, NioSelector selector) {
        setName(name);
        this.selector = selector;
    }

    @Override
    public void run() {
        while (run) {
            for (SelectionKey key : selector.keys()) {
                Object attachment = key.attachment();
                if (null == attachment) {
                    continue;
                }
                ChannelAgent agent = (ChannelAgent) attachment;
                if (System.currentTimeMillis() - agent.getLastActiveTime() > 5L * 60 * 1000) {
                    agent.destroy();
                }
            }
            try {
                if (semaphore.tryAcquire(checkIntervalSeconds, TimeUnit.SECONDS)) {
                    logger.info("server agent monitor is waked up");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}

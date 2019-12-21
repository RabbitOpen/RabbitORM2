package rabbit.open.dtx.common.nio.server;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import rabbit.open.dtx.common.nio.client.AgentMonitor;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;

/**
 * 服务端监控器
 * @author xiaoqianbin
 * @date 2019/12/18
 **/
public class ServerAgentMonitor extends AgentMonitor {

    private int checkIntervalSeconds = 10;

    private LinkedBlockingQueue<ChannelAgent> agents = new LinkedBlockingQueue<ChannelAgent>();

    public ServerAgentMonitor(String name) {
        setName(name);
    }

    public void registerMonitor(ChannelAgent agent) {
    	agents.add(agent);
    }
    
    @Override
    public void run() {
        while (run) {
            for (ChannelAgent agent : agents) {
                if (System.currentTimeMillis() - agent.getLastActiveTime() > 5L * 60 * 1000) {
                    logger.info("clear dead client connection [{}]", agent.getRemote());
                    agents.remove(agent);
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

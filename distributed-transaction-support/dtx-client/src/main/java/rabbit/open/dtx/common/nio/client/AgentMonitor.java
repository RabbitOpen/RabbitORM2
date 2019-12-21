package rabbit.open.dtx.common.nio.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.exception.NetworkException;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.protocol.KeepAlive;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 监控线程
 * @author xiaoqianbin
 * @date 2019/12/16
 **/
public class AgentMonitor extends Thread {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private DtxChannelAgentPool agentPool;

    protected Semaphore semaphore = new Semaphore(0);

    protected boolean run = true;

    private long idleMilliSecondsThreshold = 30L * 1000;

    public AgentMonitor() {

    }

    public AgentMonitor(String name, DtxChannelAgentPool agentPool) {
        super(name);
        this.agentPool = agentPool;
    }

    @Override
    public void run() {
        while (run) {
            ChannelAgent agent = null;
            try {
                agent = agentPool.getResource();
                if (System.currentTimeMillis() - agent.getLastActiveTime() > idleMilliSecondsThreshold) {
                    agent.send(new KeepAlive());
                    agent.release();
                    // 发现有空闲太久的就继续向下检测
                    continue;
                } else {
                	agent.release();
                }
            } catch (NetworkException e) {
                if (null != agent) {
                    agent.destroy();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            try {
                if (semaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                    agentPool.initConnections();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 主动唤醒monitor
     * @author  xiaoqianbin
     * @date    2019/12/18
     **/
    public void wakeup() {
        semaphore.release();
    }

    public void shutdown() {
        logger.info("agent monitor({}) is closing.....", getName());
        try {
            run = false;
            semaphore.release();
            join();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("agent monitor({}) is closed.....", getName());
    }
}

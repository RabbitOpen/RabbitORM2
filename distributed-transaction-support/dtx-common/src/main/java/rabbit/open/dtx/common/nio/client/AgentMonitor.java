package rabbit.open.dtx.common.nio.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.inter.ProtocolHandler;

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

    private ProtocolHandler protocolHandler;

    public AgentMonitor() {

    }

    public AgentMonitor(String name, DtxChannelAgentPool agentPool) {
        super(name);
        this.agentPool = agentPool;
        protocolHandler = agentPool.proxy(ProtocolHandler.class);
    }

    @Override
    public void run() {
        while (run) {
            try {
                // 获取下一个节点(空闲最久的那个)
                ChannelAgent agent = agentPool.roundList.peekNext();
                if (null != agent && System.currentTimeMillis() - agent.getLastActiveTime() > idleMilliSecondsThreshold) {
                    protocolHandler.keepAlive();
                    // 发现有空闲太久的就继续向后检测
                    continue;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            sleep10s();
        }
    }

    // 等待10秒
    private void sleep10s() {
        try {
            if (semaphore.tryAcquire(10, TimeUnit.SECONDS) && run) {
                agentPool.initConnections();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
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

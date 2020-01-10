package rabbit.open.dtx.common.nio.server.handler;

import org.springframework.util.StringUtils;
import rabbit.open.dtx.common.exception.DtxException;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.ext.AbstractNetEventHandler;
import rabbit.open.dtx.common.nio.pub.inter.ProtocolHandler;
import rabbit.open.dtx.common.nio.pub.protocol.Application;
import rabbit.open.dtx.common.nio.pub.protocol.ClientInstance;
import rabbit.open.dtx.common.nio.pub.protocol.ClusterMeta;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端简单协议实现
 * @author xiaoqianbin
 * @date 2020/1/9
 **/
public class ApplicationProtocolHandler implements ProtocolHandler, DataHandler {

    private static Map<String, Map<ChannelAgent, String>> agentCache = new ConcurrentHashMap<>();

    private AbstractServerEventHandler eventHandler;

    public ApplicationProtocolHandler(AbstractServerEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    /**
     * 心跳处理逻辑
     * @author  xiaoqianbin
     * @date    2020/1/9
     **/
    @Override
    public void keepAlive() {
        // to do： return void
    }

    /**
     * 客户端实例信息处理逻辑
     * @author  xiaoqianbin
     * @date    2020/1/9
     **/
    protected ClientInstance getClientInstanceInfo() {
        Long globalId = 0L;
        if (null != eventHandler.getTransactionHandler()) {
            globalId = eventHandler.getTransactionHandler().getNextGlobalId();
        }
        return new ClientInstance(globalId);
    }

    // 缓存agent信息
    private void cacheAgent(Application app, ChannelAgent agent) {
        if (!agentCache.containsKey(app.getName())) {
            synchronized (this) {
                if (!agentCache.containsKey(app.getName())) {
                    agentCache.put(app.getName(), new ConcurrentHashMap<>());
                }
            }
        }
        agentCache.get(app.getName()).put(agent, "");
        agent.addShutdownHook(() -> agentCache.get(agent.getAppName()).remove(agent));
    }

    /**
     * 获取app对应的客户端连接
     * @param    appName
     * @author xiaoqianbin
     * @date 2019/12/10
     **/
    public static List<ChannelAgent> getAgents(String appName) {
        Map<ChannelAgent, String> map = agentCache.get(appName);
        if (null == map || map.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(map.keySet());
    }

    @Override
    public Object process(ProtocolData protocolData) {
        if (protocolData.getData() instanceof  Application) {
            return reportApplication((Application) protocolData.getData());
        } else {
            return getClientInstanceInfo();
        }
    }

    /**
     * 客户端连接通报应用信息
     * @param    application
     * @author xiaoqianbin
     * @date 2020/1/9
     **/
    protected ClusterMeta reportApplication(Application application) {
        ChannelAgent agent = AbstractNetEventHandler.getCurrentAgent();
        agent.setAppName(application.getName());
        if (StringUtils.isEmpty(application.getName())) {
            throw new DtxException("applicationName can't be empty!");
        }
        agent.setClientInstanceId(application.getInstanceId());
        cacheAgent(application, agent);
        return null;
    }
}

package rabbit.open.dtx.common.nio.server.handler;

import org.springframework.util.StringUtils;
import rabbit.open.dtx.common.nio.exception.RpcException;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.ext.AbstractNetEventHandler;
import rabbit.open.dtx.common.nio.pub.protocol.Application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 汇报应用数据
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class ApplicationDataHandler implements DataHandler {

    private static Map<String, Map<ChannelAgent, String>> agentCache = new ConcurrentHashMap<>();

    /***
     * 心跳数据处理器
     * @param	protocolData
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    @Override
    public Object process(ProtocolData protocolData) {
        Application app = (Application) protocolData.getData();
        ChannelAgent agent = AbstractNetEventHandler.getCurrentAgent();
        agent.setAppName(app.getName());
        if (StringUtils.isEmpty(app.getName())) {
            throw new RpcException("applicationName can't be empty!");
        }
        cacheAgent(app, agent);
        return null;
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
     * @param	appName
     * @author  xiaoqianbin
     * @date    2019/12/10
     **/
    public static List<ChannelAgent> getAgents(String appName) {
        Map<ChannelAgent, String> map = agentCache.get(appName);
        if (null == map || map.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(map.keySet());
    }

}

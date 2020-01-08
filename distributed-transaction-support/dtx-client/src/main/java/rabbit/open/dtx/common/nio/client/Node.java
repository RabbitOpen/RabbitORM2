package rabbit.open.dtx.common.nio.client;

import rabbit.open.dtx.common.nio.pub.CallHelper;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.utils.NodeIdHelper;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * 服务器节点
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class Node implements Serializable {

    private String host;

    private int port;

    // 节点处于空闲状态（只有idle的节点才能新建连接）
    private boolean idle = true;

    // 被隔离
    private boolean isolated = false;

    // 绑定的agent
    private transient ChannelAgent agent;

    public Node() { }

    public Node(String host, int port) {
        if ("localhost".equals(host)) {
            CallHelper.ignoreExceptionCall(() -> this.host = InetAddress.getLocalHost().getHostAddress());
        }
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return String.format("Node{%s:%s}", host, port);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isIdle() {
        return idle;
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    public boolean isIsolated() {
        return isolated;
    }

    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
    }

    public String getId() {
        return NodeIdHelper.calcServerId(host, port);
    }

    public ChannelAgent getBoundAgent() {
        return agent;
    }

    public void bindAgent(ChannelAgent agent) {
        this.agent = agent;
    }
}

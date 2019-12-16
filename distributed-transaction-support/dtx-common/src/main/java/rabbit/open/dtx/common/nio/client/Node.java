package rabbit.open.dtx.common.nio.client;

/**
 * 服务器节点
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class Node {

    private String host;

    private int port;

    // 节点处于空闲状态（只有idle的节点才能新建连接）
    private boolean idle = true;

    // 被隔离
    private boolean isolated = false;

    public Node(String host, int port) {
        this.host = host;
        this.port = port;
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
}

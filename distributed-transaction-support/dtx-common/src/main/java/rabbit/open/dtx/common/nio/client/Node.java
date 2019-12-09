package rabbit.open.dtx.common.nio.client;

/**
 * 服务器节点
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class Node {

    private String host;

    private int port;

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
}

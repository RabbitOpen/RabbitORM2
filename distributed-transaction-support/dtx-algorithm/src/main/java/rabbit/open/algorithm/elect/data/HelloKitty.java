package rabbit.open.algorithm.elect.data;

/**
 * 心跳检测包
 * @author xiaoqianbin
 * @date 2019/12/30
 **/
public class HelloKitty implements ProtocolPacket {

    private String nodeId;

    public HelloKitty(String nodeId) {
        this();
        this.nodeId = nodeId;
    }

    public HelloKitty() {
    }

    public String getNodeId() {
        return nodeId;
    }
}

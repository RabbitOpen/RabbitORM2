package rabbit.open.algorithm.elect.data;

/**
 * leader预选结果确认
 * @author xiaoqianbin
 * @date 2020/1/14
 **/
public class PreselectionAck implements ProtocolPacket {

    private String nodeId;

    private int result = ElectionResult.AGREE;

    public PreselectionAck() {
    }

    public PreselectionAck(String nodeId) {
        this(nodeId, ElectionResult.AGREE);
    }

    public PreselectionAck(String nodeId, int result) {
        this();
        this.nodeId = nodeId;
        this.result = result;
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getResult() {
        return result;
    }
}

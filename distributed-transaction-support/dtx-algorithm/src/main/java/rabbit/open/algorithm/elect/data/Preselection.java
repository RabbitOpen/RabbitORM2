package rabbit.open.algorithm.elect.data;

/**
 * leader预选结果
 * @author xiaoqianbin
 * @date 2020/1/14
 **/
public class Preselection implements ProtocolPacket {

    // 版本号
    protected long version;

    private String nodeId;

    public Preselection() {
        super();
    }

    public Preselection(long version, String nodeId) {
        this();
        this.version = version;
        this.nodeId = nodeId;
    }

    public long getVersion() {
        return version;
    }

    public String getNodeId() {
        return nodeId;
    }
}

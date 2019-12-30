package rabbit.open.algorithm.elect.data;

/**
 * <b>@description 选举结果通知对象 </b>
 */
public class ElectedLeader implements ProtocolPacket {

	// 版本号
	protected long version;

	private String nodeId;

	public ElectedLeader() {
		super();
	}

	public ElectedLeader(long version, String nodeId) {
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

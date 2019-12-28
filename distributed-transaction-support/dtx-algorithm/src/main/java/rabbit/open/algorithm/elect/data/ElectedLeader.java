package rabbit.open.algorithm.elect.data;

/**
 * <b>@description 选举结果通知对象 </b>
 */
public class ElectedLeader extends ProtocolPacket {

	// 版本号
	protected long version;

	public ElectedLeader() {
		super();
	}

	public ElectedLeader(long version) {
		this();
		this.version = version;
	}

	public long getVersion() {
		return version;
	}

}

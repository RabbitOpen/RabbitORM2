package rabbit.open.algorithm.elect.data;

/**
 * 选举包
 * @author xiaoqianbin
 * @date 2019/11/25
 **/
public class ElectionPacket extends ProtocolPacket {

	// 版本号
	protected long version;

	public ElectionPacket() {
		super();
	}

	public ElectionPacket(long version) {
		this();
		this.version = version;
	}

	public long getVersion() {
		return version;
	}

}

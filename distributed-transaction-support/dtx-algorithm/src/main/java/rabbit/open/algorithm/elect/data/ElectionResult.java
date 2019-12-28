package rabbit.open.algorithm.elect.data;

/**
 * 结果
 * @author xiaoqianbin
 * @date 2019/11/26
 **/
public class ElectionResult extends ProtocolPacket {

	public static final int AGREE = 0; 
	
	public static final int REJECT = 1; 
	
	private int result = AGREE;
	
	// 版本号
	protected long version;

	public ElectionResult(int result, long version) {
		this();
		this.result = result;
		this.version = version;
	}

	public ElectionResult() { 
		super();
	}

	public int getResult() {
		return result;
	}

	public long getVersion() {
		return version;
	}
	
}

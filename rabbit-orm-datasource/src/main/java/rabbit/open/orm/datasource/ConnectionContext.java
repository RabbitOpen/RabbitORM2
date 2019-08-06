package rabbit.open.orm.datasource;


/**
 * <b>@description 保存获取connection时的环境信息 </b>
 */
public class ConnectionContext {

	// 获取连接时的地方(堆栈信息)
	private StackTraceElement[] stacks;
	
	// 获取连接的时间
	private long fetchMoment;

	public ConnectionContext(StackTraceElement[] stacks) {
		this.stacks = stacks;
		this.fetchMoment = System.currentTimeMillis();
	}

	public ConnectionContext() {
		this(null);
	}

	public StackTraceElement[] getStacks() {
		return stacks;
	}
	
	public long getFetchMoment() {
		return fetchMoment;
	}
}

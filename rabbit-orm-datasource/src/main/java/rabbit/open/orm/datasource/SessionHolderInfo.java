package rabbit.open.orm.datasource;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <b>@description 描述获取连接的线程信息 </b>
 */
class SessionHolderInfo {

	/**
	 * 连接获取时间
	 */
	private String fetchMoment;
	
	private String threadName;
	
	private long threadId;

	public SessionHolderInfo() {
		fetchMoment = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		threadName = Thread.currentThread().getName();
		threadId = Thread.currentThread().getId();
	}

	public String getFetchMoment() {
		return fetchMoment;
	}

	public String getThreadName() {
		return threadName;
	}

	public long getThreadId() {
		return threadId;
	}

	@Override
	public String toString() {
		return "SessionHolderInfo [fetchMoment=" + fetchMoment
				+ ", threadName=" + threadName + ", threadId=" + threadId + "]";
	}

}

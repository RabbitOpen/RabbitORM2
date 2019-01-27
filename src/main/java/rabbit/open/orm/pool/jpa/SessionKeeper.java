package rabbit.open.orm.pool.jpa;

import java.sql.Connection;

import org.apache.log4j.Logger;

public class SessionKeeper {

	private Logger logger = Logger.getLogger(getClass());
	
	private ThreadLocal<Connection> keeper = new ThreadLocal<>();

	private ThreadLocal<StackTraceElement[]> stacks = new ThreadLocal<>();
	
	private RabbitDataSource dataSource;
	
	public SessionKeeper(RabbitDataSource dataSource) {
		super();
		this.dataSource = dataSource;
	}

	public void fetchFromPool(Connection conn) {
		if (null != keeper.get()) {
			if (dataSource.isDumpSuspectedFetch()) {
				showFetchTrace();
			} else {
				logger.warn("suspective fetch operation is found!");
			}
		}
		if (dataSource.isDumpSuspectedFetch()) {
			stacks.set(Thread.currentThread().getStackTrace());
		}
		keeper.set(conn);
	}

	private void showFetchTrace() {
		StringBuilder sb = new StringBuilder("suspective fetch operation 1: \n");
		for (int i = 1; i < stacks.get().length; i++) {
			StackTraceElement ste = stacks.get()[i];
			sb.append("\t" + ste.toString() + "\r\n");
		}
		logger.error(sb.toString());
		
		StackTraceElement[] stes = Thread.currentThread().getStackTrace();
		sb = new StringBuilder("suspective fetch operation 2: \n");
		for (int i = 1; i < stes.length; i++) {
			StackTraceElement ste = stes[i];
			sb.append("\t" + ste.toString() + "\r\n");
		}
		logger.error(sb.toString());
	}
	
	public void back2Pool(Connection conn) {
		if (null == conn) {
			return;
		}
		if (conn.equals(keeper.get())) {
			keeper.remove();
			stacks.remove();
		}
	}
	
}

package rabbit.open.orm.pool.jpa;

import java.sql.Connection;

import org.apache.log4j.Logger;

public class SessionKeeper {

	private static Logger logger = Logger.getLogger(SessionKeeper.class);
	
	private ThreadLocal<Connection> keeper = new ThreadLocal<>();

	private ThreadLocal<StackTraceElement[]> stacks = new ThreadLocal<>();
	
	private RabbitDataSource dataSource;
	
	public SessionKeeper(RabbitDataSource dataSource) {
		super();
		this.dataSource = dataSource;
	}

	public void fetchFromPool(Session conn) {
		if (null != keeper.get()) {
			if (dataSource.isDumpSuspectedFetch()) {
				showFetchTrace(stacks.get());
			} else {
				logger.warn("suspective fetch operation is found!");
			}
		}
		if (dataSource.isDumpSuspectedFetch()) {
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			stacks.set(stackTrace);
			dataSource.monitor.snapshot(conn, new ConnectionContext(stackTrace));
		} else {
			dataSource.monitor.snapshot(conn, new ConnectionContext());
		}
		keeper.set(conn);
	}

	public static void showFetchTrace(StackTraceElement[] stackTraceElements) {
		StringBuilder sb = new StringBuilder("suspective fetch operation 1: \n");
		for (int i = 1; i < stackTraceElements.length; i++) {
			StackTraceElement ste = stackTraceElements[i];
			sb.append("\t" + ste.toString() + "\r\n");
		}
		logger.error(sb.toString());
		
		StackTraceElement[] stes = Thread.currentThread().getStackTrace();
		sb = new StringBuilder("current operation 2: \n");
		for (int i = 1; i < stes.length; i++) {
			StackTraceElement ste = stes[i];
			sb.append("\t" + ste.toString() + "\r\n");
		}
		logger.error(sb.toString());
	}
	
	public void back2Pool(Session conn) {
		if (null == conn) {
			return;
		}
		if (conn.equals(keeper.get())) {
			keeper.remove();
			stacks.remove();
			dataSource.monitor.removeSnapshot(conn);
		}
	}
	
}

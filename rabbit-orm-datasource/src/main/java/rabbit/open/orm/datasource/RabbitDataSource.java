package rabbit.open.orm.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.orm.common.exception.DataSourceClosedException;
import rabbit.open.orm.common.exception.GetConnectionTimeOutException;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.common.exception.RabbitORMException;

/**
 * <b>Description: 	rabbit数据源</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public class RabbitDataSource extends AbstractDataSource {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	protected LinkedBlockingDeque<Session> connectors = new LinkedBlockingDeque<>();
	
	protected DataSourceMonitor monitor;
	
	/**
	 * 标记数据源是否已经关闭
	 */
	protected boolean closed = false;
	
	// 显示慢sql
    protected boolean showSlowSql = false;
    
    // 慢sql的耗时阈值
    protected long threshold = 0L;
    
    // 打印可疑的连接获取操作堆栈信息
    protected boolean dumpSuspectedFetch = false;
    
    private SessionKeeper keeper = new SessionKeeper(this);
    
    // 数据源重启次数
    private long restartTimes = 0;
    
	/**
	 * 创建session时使用的锁
	 */
	private ReentrantLock sessionCreateLock = new ReentrantLock();
	
	// 获取连接时的等待时间
	private long fetchTimeOut = 0L;
	
	// session的最大允许持有时间，超时会打印日志(如果允许的话)
	private long maxSessionHoldingSeconds = 3L * 60;
	
	/**
	 * 计数器
	 */
	private int counter = 0;
	
	/**
	 * 获取一个可用的连接
	 */
	@Override
	public Connection getConnection() throws SQLException {
		Session conn = getConnectionInternal();
		monitor.fetchSession(conn);
		keeper.fetchFromPool(conn);
		return conn;
	}
	
	private Session getConnectionInternal() throws SQLException {
		if (closed) {
            throw new DataSourceClosedException("data source is closed!");
        }
        Session first = null;
        try {
        	first = connectors.pollFirst(fetchTimeOut, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        	logger.error(e.getMessage(), e);
        }
        if (null != first) {
            return first;
        }
        try2CreateNewSession();
        if (getCounter() < getMaxSize()) {
            return getConnectionInternal();
        } else {
            return pollConnection(15);
        }
	}

	/**
	 * 
	 * <b>Description: 获取一个连接</b><br>
	 * @param 	seconds		超时秒数
	 * @return
	 * @throws 	RabbitORMException
	 * 
	 */
    private Session pollConnection(int seconds) throws RabbitORMException {
        try {
        	Session first;
            first = connectors.pollFirst(seconds, TimeUnit.SECONDS);
            if (null == first) {
                throw new GetConnectionTimeOutException("get connection timeout for [" + seconds + "]s!");
            }
            return first;
        } catch (Exception e) {
            throw new RabbitORMException(e);
        }
    }

	/**
	 * 
	 * <b>Description: 尝试新建一个数据库连接</b><br>
	 * 
	 */
	private synchronized void try2CreateNewSession() {
		try {
			sessionCreateLock.lock();
			if (closed) {
				throw new DataSourceClosedException("data source is closed!");
			}
			if (getCounter() >= getMaxSize()) {
				return;
			}
			Session session = new Session(DriverManager.getConnection(getUrl(),
					getUsername(), getPassword()), this);
			session.setVersion(getRestartTimes());
			counter++;
			connectors.addFirst(session);
			logger.info("new session{{}} [{}] is created! [{}] session alive! [{}] sessions is idle", session.getVersion(), session, counter, connectors.size());
		} catch (Exception e) {
			throw new RabbitDMLException(e);
		} finally {
			sessionCreateLock.unlock();
		}
	}
	
	/**
	 * 
	 * <b>Description: 释放连接资源</b><br>
	 * @param conn
	 * 
	 */
	public void releaseSession(Session conn) {
		try {
			keeper.back2Pool(conn);
			monitor.releaseSession(conn);
			if (conn.getVersion() == getRestartTimes()) {
				connectors.putFirst(conn);
			} else {
				logger.error("session version[" + conn.getVersion()
						+ "] is old, current version is " + getRestartTimes());
			}
		} catch (Exception e) {
			logger.info(e.getMessage(), e);
		}
	}
	
	/**
	 * 
	 * <b>Description: 初始化</b><br>
	 * 
	 */
	@PostConstruct
	public void init() {
		loadDriverClass();
		initSessions();
		monitor = new DataSourceMonitor(this);
		monitor.start();
	}

	/**
	 * 
	 * <b>Description: 初始化连接直至满足最小连接数</b><br>
	 * 
	 */
	protected void initSessions() {
		while (getCounter() < getMinSize()) {
			try2CreateNewSession();
		}
	}

	/**
	 * 
	 * <b>Description: 加载驱动class</b><br>
	 * 
	 */
	protected void loadDriverClass() {
		try {
			Class.forName(getDriverClass());
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * 
	 * <b>Description: 获取db类型</b><br>
	 * @return
	 * 
	 */
	public DBType getDBType() {
        if (driverClass.toLowerCase().contains("oracle")) {
            return DBType.ORACLE;
        }
        if (driverClass.toLowerCase().contains("sqlserver")) {
            return DBType.SQLSERVER;
        }
        if (driverClass.toLowerCase().contains("db2")) {
            return DBType.DB2;
        }
        if (driverClass.toLowerCase().contains("mysql")) {
            return DBType.MYSQL;
        }
        if (driverClass.toLowerCase().contains("sqlite")) {
            return DBType.SQLITE;
        }
		throw new RabbitDMLException("unknown driver type[" + driverClass + "]");
	}
	
	/**
	 * 
	 * <b>Description: 销毁</b><br>
	 * 
	 */
	@PreDestroy
    public void shutdown() {
        logger.info("datasource is closing.....");
        monitor.shutdown();
        diableDataSource();
        closeAllSessions();
        logger.info("datasource is successfully closed!");
    }
	
	/**
	 * 
	 * <b>Description:	重启数据源</b><br>
	 * 
	 */
    public synchronized void restart() {
        logger.info("datasource is restarting.....");
        diableDataSource();
        closeAllSessions();
        enableDataSource();
        try {
        	setRestartTimes(getRestartTimes() + 1);
            initSessions();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        logger.info("datasource is restarted!");
    }

	private void enableDataSource() {
		setDataSourceClosed(false);
	}

	private void diableDataSource() {
		setDataSourceClosed(true);
	}

	private void setDataSourceClosed(boolean closed) {
		this.closed = closed;
	}

	/**
	 * 
	 * <b>Description: 关闭资源</b><br>
	 * 
	 */
    public void closeSession(Session session) {
        try {
            sessionCreateLock.lock();
            keeper.back2Pool(session);
            monitor.releaseSession(session);
            counter--;
            session.getConnector().close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            sessionCreateLock.unlock();
        }
    }
	
	/**
	 * 
	 * <b>Description: 关闭所有的连接</b><br>
	 * 
	 */
    private void closeAllSessions() {
        while (getCounter() > 0) {
            try {
                Session session = pollConnection(10);
                session.destroy();
            } catch (Exception e) {
                logger.error("datasource restart timeout : " + e.getMessage(), e);
                // 如果太久都没释放连接
                monitor.releaseHoldedSession();
            }
        }
    }

	public int getCounter() {
		return counter;
	}
	
	public LinkedBlockingDeque<Session> getConnectors() {
		return connectors;
	}
	
	public boolean isShowSlowSql() {
		return showSlowSql;
	}

	public void setShowSlowSql(boolean showSlowSql) {
		this.showSlowSql = showSlowSql;
	}

	public long getThreshold() {
		return threshold;
	}

	public void setThreshold(long threshold) {
		this.threshold = threshold;
	}

	public boolean isDumpSuspectedFetch() {
		return dumpSuspectedFetch;
	}

	public void setDumpSuspectedFetch(boolean dumpSuspectedFetch) {
		this.dumpSuspectedFetch = dumpSuspectedFetch;
	}
	
	public void setRestartTimes(long restartTimes) {
		this.restartTimes = restartTimes;
	}
	
	public long getRestartTimes() {
		return restartTimes;
	}
	
	public void setFetchTimeOut(long fetchTimeOut) {
		this.fetchTimeOut = fetchTimeOut;
	}
	
	public long getMaxSessionHoldingSeconds() {
		return maxSessionHoldingSeconds;
	}
	
	public void setMaxSessionHoldingSeconds(long maxSessionHoldingSeconds) {
		this.maxSessionHoldingSeconds = maxSessionHoldingSeconds;
	}
	
}

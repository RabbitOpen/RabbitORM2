package rabbit.open.orm.pool.jpa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * <b>Description: 	数据源监控线程</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public class DataSourceMonitor extends Thread {

	private boolean run = true;

	private Logger logger = Logger.getLogger(getClass());

	protected RabbitDataSource dataSource;

	private Semaphore semaphore = new Semaphore(0);

	public DataSourceMonitor(RabbitDataSource dataSource) {
		super();
		this.dataSource = dataSource;
	}

	@Override
	public void run() {
		while (run) {
			monitorDataSource();
			sleep5s();
		}
	}

	/**
	 * 
	 * <b>Description:	监控数据源</b><br>
	 * 
	 */
    private void monitorDataSource() {
        try {
            if (tooManyIdleSessions()) {
                releaseIdleSession();
                checkNetWork();
            } else {
                doKeepAlive();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

	/**
	 * 
	 * <b>Description:	保持心跳</b><br>
	 * 
	 */
    private void doKeepAlive() {
        Session last = (Session) dataSource.getConnectors().pollLast();
        if (null == last) {
            dataSource.initSessions();
            return;
        }
        if (ping(last)) {
            last.close();
        } else {
            last.destroy();
            dataSource.restart();
        }
    }

	/**
	 * 
	 * <b>Description: session个数大于最小连接数</b><br>
	 * @return
	 * 
	 */
	protected boolean tooManyIdleSessions() {
		return dataSource.getCounter() > dataSource.getMinSize();
	}

	/**
	 * 
	 * <b>Description: 释放空闲连接</b><br>
	 * 
	 */
    private void releaseIdleSession() {
        if (!tooManyIdleSessions()) {
            return;
        }
        try {
            Session tail = (Session) dataSource.getConnectors().peekLast();
            if (null == tail) {
                return;
            }
            long idle = System.currentTimeMillis() - tail.getActiveTime();
            if (idle < getMaxIdle()) {
                return;
            }
            tail = (Session) dataSource.getConnectors().pollLast();
            if (null == tail) {
                return;
            }
            idle = System.currentTimeMillis() - tail.getActiveTime();
            if (idle < getMaxIdle()) {
                dataSource.getConnectors().addLast(tail);
            } else {
                tail.destroy();
                releaseIdleSession();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    protected long getMaxIdle() {
        return 60L * 1000 * dataSource.getMaxIdle();
    }
	
	/**
	 * 
	 * <b>Description: 	检查网络连接 , 连续从尾部检测连接，发现失效的直接关闭</b><br>
	 * @return
	 * 
	 */
    private void checkNetWork() {
        Session tail = (Session) dataSource.getConnectors().pollLast();
        if (null == tail) {
            return;
        }
        if (ping(tail)) {
            dataSource.getConnectors().addLast(tail);
        } else {
            tail.destroy();
            dataSource.restart();
        }
    }

	/**
	 * 
	 * <b>Description: 	测试当前session的网络连接状况</b><br>
	 * @param session
	 * @return			true:正常；false:异常
	 * 
	 */
	private boolean ping(Session session){
	    PreparedStatement stmt = null;
		try {
			stmt = session.prepareStatement(createPingSql());
			ResultSet rs = stmt.executeQuery();
			rs.close();
			return true;
		} catch (Exception e) {
			logger.warn("network exception occurred for[" + e.getMessage() + "]");
			return false;
		} finally {
		    closeStmt(stmt);
		}
	}

    public void closeStmt(PreparedStatement stmt) {
        try {
            if (null != stmt) {
                stmt.close();
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String createPingSql() {
        String pingSql = " select 1 ";
        if (DBType.ORACLE.equals(dataSource.getDBType())) {
            pingSql = " select 1 from dual ";
        } 
        if (DBType.DB2.equals(dataSource.getDBType())) {
            pingSql = " values 1 ";
        }
        return pingSql;
    }
	

	/**
	 * 
	 * <b>Description: 睡眠5s</b><br>
	 * 
	 */
	protected void sleep5s() {
		try {
			semaphore.tryAcquire(5, TimeUnit.SECONDS);
		} catch (Exception e) {
			logger.error("database monitor is interrupted");
		}
	}
	
	public void shutdown(){
		logger.info("datasource monitor is stopping....");
		run = false;
		try {
			semaphore.release();
			join();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.info("datasource monitor is stopped!");
	}

}

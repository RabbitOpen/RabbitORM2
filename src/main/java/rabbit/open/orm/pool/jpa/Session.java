package rabbit.open.orm.pool.jpa;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

/**
 * <b>Description: 连接对象</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
public class Session extends AbstractConnection {

	private static final Integer DO_NOTHING = 9999;

	// 连接对象
	private Connection conn;

	// 上次活跃时间
	private long activeTime = 0;

	// 数据源
	private RabbitDataSource dataSource;
	
	// 事务隔离级别
	private int transactionIsolation = 0;

	// 缓存PreparedStatement
	private LinkedHashMap<String, PreparedStatement> cachedStmts;

	protected Logger logger = Logger.getLogger(getClass());
	
	// 代表数据源重启的次数
	private long version = 0;

	// 标记sql异常
	private static ThreadLocal<Object> sqlExceptionContext = new ThreadLocal<>();

	public Session(Connection conn, RabbitDataSource dataSource) throws SQLException {
		super();
		cachedStmts = new LinkedHashMap<>();
		this.conn = conn;
		this.transactionIsolation = conn.getTransactionIsolation();
		this.dataSource = dataSource;
		activeTime = System.currentTimeMillis();
	}
	
	public Session() {
	}
	
	public static Throwable getRootCause(Exception e) {
        Throwable cause = e;
        while(null != cause.getCause()) {
            cause = cause.getCause();
        }
        return cause;
    }

	public static boolean hasSQLException() {
		return null != sqlExceptionContext.get();
	}

	public static void flagException() {
		sqlExceptionContext.set(true);
	}

	/**
	 * <b>Description 清除异常context</b>
	 */
	public static void clearException() {
		sqlExceptionContext.remove();
	}

	/**
	 * 释放连接回连接池
	 */
	@Override
	public void close() {
		if (hasSQLException()) {
			destroy();
		} else {
			releaseSession();
		}
	}

	private void releaseSession() {
		activeTime = System.currentTimeMillis();
		dataSource.releaseSession(this);
	}

	@Override
	public boolean isClosed() throws SQLException {
		return conn.isClosed();
	}

	@Override
	public void commit() throws SQLException {
		if (!conn.getAutoCommit()) {
			conn.commit();
		}
	}

	@Override
	public void rollback() throws SQLException {
		if (!conn.getAutoCommit()) {
			conn.rollback();
		}
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return conn.getAutoCommit();
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		if (autoCommit == conn.getAutoCommit()) {
			return;
		}
		conn.setAutoCommit(autoCommit);
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return conn.getMetaData();
	}

	@Override
	public Statement createStatement() throws SQLException {
		return conn.createStatement();
	}

	/**
	 * 
	 * <b>Description: 获取与session绑定的连接对象</b><br>
	 * 
	 * @return
	 * 
	 */
	public Connection getConnector() {
		return conn;
	}

	public long getActiveTime() {
		return activeTime;
	}

	private void closeStmts() {
		for (PreparedStatement stmt : cachedStmts.values()) {
			closeRealStmt(stmt);
		}
	}

	/**
	 * 
	 * <b>Description: 关闭真实的PreparedStatement</b><br>
	 * 
	 * @param stmt
	 * 
	 */
	private void closeRealStmt(PreparedStatement stmt) {
		try {
			for (Field field : stmt.getClass().getDeclaredFields()) {
				field.setAccessible(true);
				Object fv = field.get(stmt);
				if (!(fv instanceof PreparedStatementProxy)) {
					continue;
				}
				PreparedStatementProxy psp = (PreparedStatementProxy) fv;
				psp.destroy();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * 
	 * <b>Description: 关闭连接</b><br>
	 * 
	 */
	public void destroy() {
		closeStmts();
		dataSource.closeSession(this);
		logger.info("session{version: " + version + ", stmtments:" + cachedStmts.size() + "} closed, ["
				+ dataSource.getCounter() + "] session left!");
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return prepareStatement(sql, DO_NOTHING);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int genKey)
			throws SQLException {
		String sqlKey = createSqlKey(sql);
		PreparedStatement cachedStmt = cachedStmts.remove(sqlKey);
		if (null != cachedStmt) {
			cachedStmts.put(sqlKey, cachedStmt);
			return cachedStmt;
		}
		if (cachedStmts.size() == dataSource.getMaxCachedStmt()) {
			PreparedStatement idleStmt = cachedStmts.remove(cachedStmts
					.keySet().iterator().next());
			closeRealStmt(idleStmt);
		}
		PreparedStatement stmt = null;
		if (DO_NOTHING == genKey) {
			stmt = PreparedStatementProxy.getProxy(conn.prepareStatement(sql), sql, dataSource);
		} else {
			stmt = PreparedStatementProxy.getProxy(conn.prepareStatement(sql, genKey), sql, dataSource);
		}
		cachedStmts.put(sqlKey, stmt);
		return stmt;
	}

	private String createSqlKey(String sql) {
		return sql.replaceAll(" ", "");
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		conn.setReadOnly(readOnly);
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return conn.isReadOnly();
	}
	
	@Override
	public Savepoint setSavepoint() throws SQLException {
		return conn.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		return conn.setSavepoint(name);
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		if (!conn.getAutoCommit()) {
			conn.rollback(savepoint);
		}
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		if (!conn.getAutoCommit()) {
			conn.releaseSavepoint(savepoint);
		}
	}
	
	public void setVersion(long version) {
		this.version = version;
	}
	
	public long getVersion() {
		return version;
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		return transactionIsolation;
	}
	
	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		if (getTransactionIsolation() != level) {
			conn.setTransactionIsolation(level);
			this.transactionIsolation = level;
		}
	}
}

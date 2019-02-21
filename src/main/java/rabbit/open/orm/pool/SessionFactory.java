package rabbit.open.orm.pool;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.ddl.DDLType;
import rabbit.open.orm.dialect.ddl.DDLHelper;
import rabbit.open.orm.dialect.dml.DeleteDialectAdapter;
import rabbit.open.orm.dialect.dml.DialectType;
import rabbit.open.orm.dml.DialectTransformer;
import rabbit.open.orm.dml.PolicyInsert;
import rabbit.open.orm.dml.filter.DMLFilter;
import rabbit.open.orm.dml.filter.DMLType;
import rabbit.open.orm.dml.filter.PreparedValue;
import rabbit.open.orm.dml.name.SQLParser;
import rabbit.open.orm.pool.jpa.CombinedDataSource;
import rabbit.open.orm.pool.jpa.SessionProxy;
import rabbit.open.orm.spring.TransactionObject;

public class SessionFactory {

    // 数据源
    protected DataSource dataSource;

    //复合数据源
    protected CombinedDataSource combinedDataSource;
    
    // 是否显示sql
    protected boolean showSql = false;

    // 是否格式化sql
    protected boolean formatSql = false;
    
    // 是否显示真实的预编译sql
    protected boolean maskPreparedSql = false;
    
    // 是否扫描classpath 中的jar包
    private boolean scanJar = false;

    // ddl类型
    protected String ddl = DDLType.NONE.name();
    
    protected DMLFilter filter;

    // 方言
    protected String dialect;

    private String mappingFiles;

    // 需要扫描的包
    protected String packages2Scan = "";

    private static Logger logger = Logger.getLogger(SessionFactory.class);

    public static final ThreadLocal<Object> transObjHolder = new ThreadLocal<>();
    
    // 内嵌事务的事务对象
    private static final ThreadLocal<Object> nestedTransObj = new ThreadLocal<>();

    private Set<DataSource> sources = new HashSet<>();
    
    public Connection getConnection() throws SQLException {
        return getConnection(null, null, null);
    }

    public Connection getConnection(Class<?> entityClz, String tableName,
            DMLType type) throws SQLException {
        DataSource ds = getDataSource(entityClz, tableName, type);
        Connection conn;
        if (isTransactionOpen()) {
			RabbitConnectionHolder holder = (RabbitConnectionHolder) TransactionSynchronizationManager.getResource(ds);
			if (holder.hasConnection()) {
				conn = holder.getConnection();
			} else {
				conn = ds.getConnection();
				holder.setConnection(conn);
			}
			conn = SessionProxy.getProxy(conn);
			if (conn.getAutoCommit()) {
				conn.setAutoCommit(false);
			}
			cacheSavepoint(conn);
			return conn;
		} else {
			conn = ds.getConnection();
			conn = SessionProxy.getProxy(conn);
			if (!conn.getAutoCommit()) {
				conn.setAutoCommit(true);
			}
			return conn;
		}
    }

	/**
	 * <b>@description 缓存回滚点 </b>
	 * @param conn
	 * @throws SQLException
	 */
	private void cacheSavepoint(Connection conn) throws SQLException {
		if (null != nestedTransObj.get()) {
			Savepoint sp = conn.setSavepoint();
			TransactionObject tranObj = (TransactionObject) nestedTransObj.get();
			tranObj.setSavePoint(sp);
			tranObj.setConnection(conn);
			nestedTransObj.remove();
		}
	}

    /**
     * <b>Description   获取一个数据源</b>
     * @param entityClz
     * @param tableName
     * @param type
     * @return
     */
    private DataSource getDataSource(Class<?> entityClz, String tableName,
            DMLType type) {
        if (null != combinedDataSource) {
            return combinedDataSource.getDataSource(entityClz, tableName, type);
        }
        return dataSource;
    }
    
    /**
     * <b>@description 获取sessionfactory能够支配的所有数据源 </b>
     * @return
     */
    private Set<DataSource> getAllDataSources() {
    	if (!sources.isEmpty()) {
    		return sources;
    	}
    	if (null != combinedDataSource) {
    		sources.addAll(combinedDataSource.getAllDataSources());
        } else {
        	sources.add(dataSource);
        }
    	return sources;
    }
    
    public void setCombinedDataSource(CombinedDataSource combinedDataSource) {
        this.combinedDataSource = combinedDataSource;
    }

    // 开启事务
    public static void beginTransaction(Object obj, SessionFactory factory) {
        if (null == transObjHolder.get()) {
        	initSessionHolder(factory);
            transObjHolder.set(obj);
            nestedTransObj.remove();
        } else {
        	TransactionObject tObj = (TransactionObject) obj;
        	if (TransactionDefinition.PROPAGATION_NESTED == tObj.getPropagation()) {
        		nestedTransObj.set(obj);
        	}
        }
    }

	/**
	 * <b>@description 初始化connectionHolder给框架使用 </b>
	 * @param factory
	 */
	private static void initSessionHolder(SessionFactory factory) {
		for (DataSource ds : factory.getAllDataSources()) {
			ConnectionHolder holder = new RabbitConnectionHolder();
			holder.setSynchronizedWithTransaction(true);
			if (null == TransactionSynchronizationManager.getResource(ds)) {
				TransactionSynchronizationManager.bindResource(ds, holder);
			}
		}
	}

    /**
     * 
     * <b>Description: 提交事务</b><br>
     * @param transactionObject 事务对象
     * @param factory 
     * 
     */
    public static void commit(Object transactionObject, SessionFactory factory) {
        if (null == transObjHolder.get() || !transObjHolder.get().equals(transactionObject)) {
            return;
        }
        transObjHolder.remove();
        for (DataSource ds : factory.getAllDataSources()) {
        	RabbitConnectionHolder holder = (RabbitConnectionHolder) TransactionSynchronizationManager
					.getResource(ds);
        	if (!holder.hasConnection()) {
        		continue;
        	}
			try {
				holder.getConnection().commit();
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			} finally {
				try {
					holder.getConnection().close();
				} catch (SQLException e) {
					logger.error(e.getMessage(), e);
				}
			}
        	TransactionSynchronizationManager.unbindResource(ds);
		}
    }

    /**
     * 
     * <b>Description: 回滚操作</b><br>
     * @param transactionObj
     * @param factory
     * 
     */
    public static void rollBack(Object transactionObj, SessionFactory factory) {
        if (null == transObjHolder.get()) {
            return;
        }
        if (transObjHolder.get().equals(transactionObj)) {
        	rollbackAll(factory);
        } else {
        	rollBackToSavepoint(transactionObj);
        }
    }

	/**
	 * <b>@description 回滚到指定的savePoint </b>
	 * @param transactionObj
	 */
	private static void rollBackToSavepoint(Object transactionObj) {
		TransactionObject tObj = (TransactionObject) transactionObj;
		Savepoint sp = tObj.getSavePoint();
		if (null != sp) {
			try {
				tObj.getConnection().rollback(sp);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private static void rollbackAll(SessionFactory factory) {
		transObjHolder.remove();
		for (DataSource ds : factory.getAllDataSources()) {
			RabbitConnectionHolder holder = (RabbitConnectionHolder) TransactionSynchronizationManager
					.getResource(ds);
			if (!holder.hasConnection()) {
        		continue;
        	}
			try {
				holder.getConnection().rollback();
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			} finally {
				try {
					holder.getConnection().close();
				} catch (SQLException e) {
					logger.error(e.getMessage(), e);
				}
			}
			TransactionSynchronizationManager.unbindResource(ds);
		}
	}

    /**
     * 
     * <b>Description: 释放连接到连接池</b><br>
     * @param conn
     * 
     */
    public static void releaseConnection(Connection conn) {
        if (null == conn) {
            return;
        }
        if (isTransactionOpen()) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static boolean isTransactionOpen() {
        return null != transObjHolder.get();
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 
     * <b>Description: 执行ddl操作</b><br>
     * 
     */
    @PostConstruct
    public void setUp() {
        DialectTransformer.init();
        DeleteDialectAdapter.init();
        PolicyInsert.init();
        DDLHelper.checkMapping(this, getPackages2Scan());
        DDLHelper.init();
        //组合数据源不支持ddl
        if (null == combinedDataSource) {
            DDLHelper.executeDDL(this, getPackages2Scan());
        }
        if (!isEmpty(mappingFiles)) {
            new SQLParser(mappingFiles).doXmlParsing();
        }
    }

    public static boolean isEmpty(String str) {
        return null == str || "".equals(str.trim());
    }

    public boolean isShowSql() {
        return showSql;
    }

    public void setShowSql(boolean showSql) {
        this.showSql = showSql;
    }

    public boolean isFormatSql() {
        return formatSql;
    }

    public void setFormatSql(boolean formatSql) {
        this.formatSql = formatSql;
    }

    public String getDdl() {
        return ddl;
    }

    public void setDdl(String ddl) {
        this.ddl = ddl;
    }

    public String getPackages2Scan() {
        return packages2Scan;
    }

    public void setPackages2Scan(String packages2Scan) {
        this.packages2Scan = packages2Scan;
    }

    public DialectType getDialectType() {
        return DialectType.format(dialect);
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public void setMappingFiles(String mappingFiles) {
        this.mappingFiles = mappingFiles;
    }

    public boolean isMaskPreparedSql() {
        return maskPreparedSql;
    }

    public void setMaskPreparedSql(boolean maskPreparedSql) {
        this.maskPreparedSql = maskPreparedSql;
    }

    public void setFilter(DMLFilter filter) {
        this.filter = filter;
    }
    
    public Object onValueSetted(PreparedValue pv, DMLType dmlType) {
        if (null == pv) {
            return null;
        }
        if (null == dmlType) {
            return pv.getValue();
        }
        if (null == pv.getField()) {
            return pv.getValue();
        }
        if (null != filter) {
            return filter.onValueSetted(pv.getValue(), pv.getField(), dmlType);
        }
        return pv.getValue();
    }

    public Object onValueGetted(Object value, Field field) {
        if (null != filter) {
            return filter.onValueGetted(value, field);
        }
        return value;
    }
    /**
     * <b>Description  获取字段名</b>
     * @param col
     * @return
     */
    public String getColumnName(Column col) {
        return DDLHelper.getCurrentDDLHelper(this).getColumnName(col);
    }

	public boolean isScanJar() {
		return scanJar;
	}
	
	public void setScanJar(boolean scanJar) {
		this.scanJar = scanJar;
	}
}

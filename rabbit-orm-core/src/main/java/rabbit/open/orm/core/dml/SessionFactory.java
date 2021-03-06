package rabbit.open.orm.core.dml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import rabbit.open.orm.common.ddl.DDLType;
import rabbit.open.orm.common.dialect.DialectType;
import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.dialect.ddl.DDLHelper;
import rabbit.open.orm.core.dialect.dml.DeleteDialectAdapter;
import rabbit.open.orm.core.dml.interceptor.DMLInterceptor;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.name.NamedSQL;
import rabbit.open.orm.core.dml.shard.ShardedNameMatcher;
import rabbit.open.orm.core.dml.shard.ShardingPolicy;
import rabbit.open.orm.core.dml.shard.impl.DefaultShardedNameMatcher;
import rabbit.open.orm.core.spring.TransactionObject;
import rabbit.open.orm.core.utils.PackageScanner;
import rabbit.open.orm.core.utils.XmlMapperParser;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

	// ddl类型
	protected String ddl = DDLType.NONE.name();

	protected DMLInterceptor interceptor;

	// 方言
	protected String dialect;

	private String mappingFiles;

	// 需要扫描的包
	protected String packages2Scan = "";

	private static Logger logger = LoggerFactory.getLogger(SessionFactory.class);

	public static final ThreadLocal<Object> transObjHolder = new ThreadLocal<>();

	// 内嵌事务的事务对象
	private static final ThreadLocal<Object> nestedTransObj = new ThreadLocal<>();

	// 缓存各个数据源的默认事务隔离级别
	private Map<DataSource, Integer> defaultIsolationLevelHolder = new ConcurrentHashMap<>();

	private Set<DataSource> sources = new HashSet<>();

	private XmlMapperParser sqlParser;

	// 实体类的包名路径
	private Set<String> entities;
	
	// 分区表监控器
	private ShardedTableMonitor shardedTableMonitor;

	// 分片策略和分片表集合之间的映射关系缓存
	private Map<Class<? extends ShardingPolicy>, Map<Class<?>, List<TableMeta>>> shardedTablesCache = new ConcurrentHashMap<>();

	private ShardedNameMatcher shardedNameMatcher;

	public Connection getConnection() throws SQLException {
		return getConnection(null, null, null);
	}

	public Connection getConnection(Class<?> entityClz, TableMeta tableMeta, DMLType type) throws SQLException {
		DataSource ds = getDataSource(entityClz, tableMeta, type);
		Connection conn;
		if (isTransactionOpen()) {
			RabbitConnectionHolder holder = (RabbitConnectionHolder) TransactionSynchronizationManager
					.getResource(ds);
			if (holder.hasConnection()) {
				conn = holder.getConnection();
			} else {
				conn = ds.getConnection();
				holder.setConnection(conn);
			}
			conn = SessionProxy.getProxy(conn);
			disableAutoCommit(conn);
			cacheSavepoint(conn);
			return conn;
		} else {
			conn = ds.getConnection();
			conn = SessionProxy.getProxy(conn);
			enableAutoCommit(conn);
			return conn;
		}
	}

	/**
	 * <b>@description 设置事务隔离级别  </b>
	 * @param conn
	 * @param ds
	 */
	public void setTransactionIsolation(Connection conn, DataSource ds) {
		TransactionObject tObj = (TransactionObject) transObjHolder.get();
		if (null == tObj) {
			return;
		}
		int requiredIsolationLevel = tObj.getTransactionIsolationLevel();
		if (requiredIsolationLevel == TransactionDefinition.ISOLATION_DEFAULT) {
			return;
		}
		if (requiredIsolationLevel != defaultIsolationLevelHolder.get(ds)) {
			try {
				conn.setTransactionIsolation(requiredIsolationLevel);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * <b>@description 禁止自动提交  </b>
	 * @param conn
	 */
	public static void disableAutoCommit(Connection conn) {
		try {
			if (conn.getAutoCommit()) {
				conn.setAutoCommit(false);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * <b>@description 允许自动提交 </b>
	 * @param conn
	 */
	public static void enableAutoCommit(Connection conn) {
		try {
			if (!conn.getAutoCommit()) {
				conn.setAutoCommit(true);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * <b>@description 缓存回滚点 </b>
	 * @param conn
	 */
	private void cacheSavepoint(Connection conn) {
		if (null != nestedTransObj.get()) {
			Savepoint sp = null;
			try {
				sp = conn.setSavepoint();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			TransactionObject tranObj = (TransactionObject) nestedTransObj.get();
			tranObj.setSavePoint(sp);
			tranObj.setConnection(conn);
			nestedTransObj.remove();
		}
	}

	/**
	 * <b>Description   获取一个数据源</b>
	 * @param entityClz
	 * @param tableMeta
	 * @param type
	 * @return
	 */
	private DataSource getDataSource(Class<?> entityClz, TableMeta tableMeta,
									 DMLType type) {
		if (null != combinedDataSource) {
			return combinedDataSource.getDataSource(entityClz, tableMeta, type);
		}
		return dataSource;
	}

	/**
	 * <b>@description 获取sessionfactory能够支配的所有数据源 </b>
	 * @return
	 */
	public Set<DataSource> getAllDataSources() {
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
			RabbitConnectionHolder holder = new RabbitConnectionHolder();
			holder.setSynchronizedWithTransaction(true);
			holder.setFactory(factory);
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
		TransactionObject tObj = (TransactionObject) transObjHolder.get();
		int requiredIsolationLevel = tObj.getTransactionIsolationLevel();
		transObjHolder.remove();
		for (DataSource ds : factory.getAllDataSources()) {
			RabbitConnectionHolder holder = (RabbitConnectionHolder) TransactionSynchronizationManager
					.getResource(ds);
			if (!holder.hasConnection()) {
				continue;
			}
			try {
				holder.getConnection().commit();
				resetIsolationLevel(factory, requiredIsolationLevel, ds, holder);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				try {
					holder.getConnection().close();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
			TransactionSynchronizationManager.unbindResource(ds);
		}
	}

	/**
	 * <b>@description 重置连接的事务隔离级别 </b>
	 * @param factory
	 * @param requiredIsolationLevel 当前事务要求的事务隔离级别
	 * @param ds
	 * @param holder
	 * @throws SQLException
	 */
	private static void resetIsolationLevel(SessionFactory factory,
											int requiredIsolationLevel, DataSource ds,
											RabbitConnectionHolder holder) throws SQLException {
		if (requiredIsolationLevel != TransactionDefinition.ISOLATION_DEFAULT) {
			// 数据源默认的事务隔离级别
			Integer defaultIsolationLevel = factory.defaultIsolationLevelHolder.get(ds);
			if (requiredIsolationLevel != defaultIsolationLevel) {
				holder.getConnection().setTransactionIsolation(defaultIsolationLevel);
			}
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
		TransactionObject tObj = (TransactionObject) transObjHolder.get();
		int requiredIsolationLevel = tObj.getTransactionIsolationLevel();
		transObjHolder.remove();
		for (DataSource ds : factory.getAllDataSources()) {
			RabbitConnectionHolder holder = (RabbitConnectionHolder) TransactionSynchronizationManager
					.getResource(ds);
			if (!holder.hasConnection()) {
				continue;
			}
			try {
				holder.getConnection().rollback();
				resetIsolationLevel(factory, requiredIsolationLevel, ds, holder);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				try {
					holder.getConnection().close();
				} catch (Exception e) {
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
	 * @throws ClassNotFoundException 
	 *
	 */
	@PostConstruct
	public void setUp() throws ClassNotFoundException {
		DialectTransformer.init();
		DeleteDialectAdapter.init();
		PolicyInsert.init();
		checkMapping();
		cacheFieldsAlias();
		DDLHelper.init();
		cacheDefaultIsolationLevel();
		//组合数据源不支持ddl
		if (null == combinedDataSource) {
			DDLHelper.executeDDL(this);
		}
		if (!isEmpty(mappingFiles)) {
			sqlParser = new XmlMapperParser(mappingFiles);
			sqlParser.doXmlParsing();
		}
		startShardedTableMonitor();
	}

	/**
	 * <b>@description 启动分区表监控线程  </b>
	 * @throws ClassNotFoundException 
	 */
	private void startShardedTableMonitor() throws ClassNotFoundException {
		// 预加载一下所有的数据源
		getAllDataSources();
		shardedTableMonitor = new ShardedTableMonitor(this);
		// 预先加载一次分片表
		shardedTableMonitor.reloadShardedTables();
		shardedTableMonitor.start();
	}
	
	@PreDestroy
	public void destroy() {
		shardedTableMonitor.shutdown();
	}

	 /**
     * @description 检查包和类的映射关系
     */
    private void checkMapping() {
		for (String clzName : (HashSet<String>) getEntities()) {
    		try {
				Class<?> clz = Class.forName(clzName);
				MetaData.getMetaByClass(clz);
			} catch (ClassNotFoundException e) {
				logger.error(e.getMessage(), e);
			}
    	}
    }
    
    /**
     * @description 缓存字段别名映射关系
     */
	private void cacheFieldsAlias() {
		for (String clzName : (HashSet<String>) getEntities()) {
			try {
				Class<?> clz = Class.forName(clzName);
				Map<String, String> aliasMappings = MetaData.getFieldsAliasMapping(clz);
				if (null == aliasMappings) {
					aliasMappings = new ConcurrentHashMap<>();
					MetaData.setFieldsAliasMapping(clz, aliasMappings);
				}
				Collection<FieldMetaData> fieldsMetas = MetaData.getCachedFieldsMetas(clz).values();
				int i = 0;
				for (FieldMetaData metaData : fieldsMetas) {
					String fn = metaData.getField().getName();
					String alias = Integer.toString(i);
					aliasMappings.put(alias, fn);
					aliasMappings.put(fn, alias);
					i++;
				}
			} catch (ClassNotFoundException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
    
	
	/**
	 *
	 * <b>Description:	根据查询的名字和类信息获取命名查询对象</b><br>
	 * @param name		定义的查询名字
	 * @param clz		namespace对应的class
	 * @return
	 *
	 */
	public NamedSQL getQueryByNameAndClass(String name, Class<?> clz) {
		return sqlParser.getQueryByNameAndClass(name, clz);
	}

	/**
	 * <b>@description 缓存默认事务隔离级别 </b>
	 */
	private void cacheDefaultIsolationLevel() {
		for (DataSource ds : getAllDataSources()) {
			Connection conn = null;
			try {
				conn = ds.getConnection();
				defaultIsolationLevelHolder.put(ds, conn.getTransactionIsolation());
			} catch (Exception e) {
				throw new RabbitDMLException(e);
			} finally {
				DMLObject.closeConnection(conn);
			}
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

	public void setInterceptor(DMLInterceptor filter) {
		this.interceptor = filter;
	}

	public Object onValueSet(PreparedValue pv, DMLType dmlType) {
		if (null == pv) {
			return null;
		}
		if (null == dmlType) {
			return pv.getValue();
		}
		if (null == pv.getField()) {
			return pv.getValue();
		}
		if (null != interceptor) {
			return interceptor.onValueSet(pv.getValue(), pv.getField(), dmlType);
		}
		return pv.getValue();
	}

	public Object onValueGot(Object value, Field field) {
		if (null != interceptor) {
			return interceptor.onValueGot(value, field);
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

	public Map<DataSource, Integer> getDefaultIsolationLevelHolder() {
		return defaultIsolationLevelHolder;
	}

	/**
	 * <b>@description 获取映射的实体类 </b>
	 * @return
	 */
	public Set<String> getEntities() {
		if (null == entities) {
			entities = PackageScanner.filterByAnnotation(getPackages2Scan().split(","),
					Entity.class);
		}
		return entities;
	}
	
	public void setShardedNameMatcher(ShardedNameMatcher nameMatcher) {
		this.shardedNameMatcher = nameMatcher;
	}
	
	public ShardedNameMatcher getShardedNameMatcher() {
		if (null == shardedNameMatcher) {
			setShardedNameMatcher(new DefaultShardedNameMatcher());
		}
		return shardedNameMatcher;
	}
	
	/**
	 * <b>@description  设置分片策略对应的表信息 </b>
	 * @param clz
	 * @param entityClz
	 * @param tabMetas
	 */
	public void setShardTables(Class<? extends ShardingPolicy> clz, Class<?> entityClz, List<TableMeta> tabMetas) {
		if (!this.shardedTablesCache.containsKey(clz)) {
			this.shardedTablesCache.put(clz, new ConcurrentHashMap<>());
		}
		this.shardedTablesCache.get(clz).put(entityClz, tabMetas);
	}
	
	public List<TableMeta> getTableMetas(Class<? extends ShardingPolicy> clz, Class<?> entityClz) {
		return shardedTablesCache.get(clz).get(entityClz);
	}
}

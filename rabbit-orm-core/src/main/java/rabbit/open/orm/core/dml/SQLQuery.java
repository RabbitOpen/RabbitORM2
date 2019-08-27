package rabbit.open.orm.core.dml;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.common.exception.UnKnownFieldException;
import rabbit.open.orm.common.shard.ShardFactor;
import rabbit.open.orm.core.dml.filter.PreparedValue;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.name.NamedSQL;
import rabbit.open.orm.datasource.Session;

/**
 * 
 * jdbc命名sql是原生sql，查询出来的数据通过字段名和对象属性进行匹配 
 * <b>@description jdbc命名sql查询对象 </b>
 * @param <T>
 */
public class SQLQuery<T> extends DMLAdapter<T> {


	protected NamedSQL namedObject;

	private DMLType dmlType;

	private SQLOperation sqlOpr;

	protected NamedSQL nameObject;

	private TreeMap<Integer, PreparedValue> fieldsValues = new TreeMap<>();

	/**
	 * @param sessionFactory
	 * @param clz			 需要返回的对象
	 * @param queryName
	 */
	public SQLQuery(SessionFactory sessionFactory, Class<T> clz, String queryName) {
		super(sessionFactory, clz);
		this.dmlType = DMLType.SELECT;
		this.sessionFactory = sessionFactory;
		this.namedObject = sessionFactory.getQueryByNameAndClass(queryName, clz);
	}

	/**
	 * 如果命名sql指定了查询的主表名，就使用sql指定的，否则使用entity注解声明的表名
	 * @return
	 */
	@Override
	protected String getCurrentTableName() {
		return getCurrentTableNameByNamedObject(namedObject);
	}

	private Object execute() {
		sql = new StringBuilder(namedObject.getSql());
		Connection conn = null;
		try {
			conn = sessionFactory.getConnection(getEntityClz(), getCurrentTableName(), dmlType);
			return sqlOpr.executeSQL(conn);
		} catch (UnKnownFieldException e) {
			throw e;
		} catch (Exception e) {
			showUnMaskedSql(false);
			Session.flagException();
			throw new RabbitDMLException(e.getMessage(), e);
		} finally {
			closeConnection(conn);
			Session.clearException();
		}
	}

	/**
	 * <b>@description 准备预编译sql的值 </b>
	 */
	protected void setPreparedValues() {
		for (PreparedValue v : fieldsValues.values()) {
			preparedValues.add(v);
		}
	}

	/**
	 * <b>@description 给变量设值</b>
	 * 
	 * @param fieldAlias 变量别名
	 * @param value      变量的值
	 * @return
	 */
	public SQLQuery<T> set(String fieldAlias, Object value) {
		List<Integer> indexes = namedObject.getFieldIndexes(fieldAlias);
		for (int index : indexes) {
			fieldsValues.put(index, new PreparedValue(value));
		}
		return this;
	}

	/**
	 * <b>@description 读一行数据 </b>
	 * @return
	 */
	public T unique() {
		List<T> list = list();
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * <b>@description 查询列表 </b>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<T> list() {
		sqlOpr = createQueryOperation();
		return (List<T>)execute();
	}

	/**
	 * <b>@description 读取结果集 </b>
	 * @param rs
	 * @throws SQLException
	 */
	private List<T> readResults(ResultSet rs) throws SQLException {
		List<T> list = new ArrayList<>();
		List<String> headers = getColumnNames(rs);
		while (rs.next()) {
			T targetObj = DMLAdapter.newInstance(getEntityClz());
			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				Object colValue = rs.getObject(i);
				if (null == colValue) {
					continue;
				}
				if (sessionFactory.getDialectType().isOracle() && colValue instanceof Date) {
					colValue = rs.getTimestamp(i);
				}
				Field field = getFieldByColumnName(headers.get(i - 1));
				if (null != field) {
					colValue = sessionFactory.onValueGot(colValue, field);
					DialectTransformer.getTransformer(sessionFactory.getDialectType()).setValue2EntityField(targetObj,
							field, colValue);
				}
			}
			list.add(targetObj);
		}
		return list;
	}

	/**
	 * 根据数据库列名获取字段对象
	 * <b>@description  </b>
	 * @param colName
	 * @return
	 */
	private Field getFieldByColumnName(String colName) {
		try {
			return MetaData.getCachedFieldsMeta(getEntityClz(), colName).getField();
		} catch (UnKnownFieldException e) {
			try {
				return getEntityClz().getDeclaredField(colName);
			} catch (NoSuchFieldException nfe) {
				return null;
			}
		}
	}

	/**
	 * <b>@description 根据结果集获取列名信息 </b>
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private List<String> getColumnNames(ResultSet rs) throws SQLException {
		List<String> headers = new ArrayList<>();
		for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
			headers.add(rs.getMetaData().getColumnLabel(i));
		}
		return headers;
	}

	private PreparedStatement prepareStatement(Connection conn) throws SQLException {
		sql = new StringBuilder(namedObject.getSql());
		PreparedStatement stmt = conn.prepareStatement(sql.toString());
		setPreparedStatementValue(stmt, dmlType);
		showSql();
		return stmt;
	}
	
	/**
	 * <b>@description 创建查询操作  </b>
	 * @return
	 */
	private SQLOperation createQueryOperation() {
		return new SQLOperation() {
			@Override
			public Object executeSQL(Connection conn) throws SQLException {
				PreparedStatement stmt = null;
				try {
					setPreparedValues();
					stmt = prepareStatement(conn);
					ResultSet rs = stmt.executeQuery();
					List<T> list = readResults(rs);
					rs.close();
					return list;
				} finally {
					closeStmt(stmt);
				}
			}
		};
	}
	
	
	@Override
	protected List<ShardFactor> getFactors() {
		return factors;
	}

	@Override
	protected String getAliasByTableName(String tableName) {
		return tableName;
	}

	protected interface SQLOperation {

		/**
		 * 
		 * <b>Description: 执行sql操作</b><br>
		 * @param conn
		 * @throws Exception
		 * 
		 */
		public Object executeSQL(Connection conn) throws SQLException;
	}
}

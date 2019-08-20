package rabbit.open.orm.core.dml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.TreeMap;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.core.dml.filter.PreparedValue;
import rabbit.open.orm.core.dml.name.NamedSQL;

/**
 * <b>@description 命名更新对象 </b>
 */
public class NamedUpdate<T> extends NonQueryAdapter<T> {

	protected NamedSQL namedObject;
	
	private TreeMap<Integer, PreparedValue> fieldsValues;
	
	public NamedUpdate(SessionFactory fatory, Class<T> clz, String name) {
		this(fatory, clz, name, DMLType.UPDATE);
	}

	protected NamedUpdate(SessionFactory factory, Class<T> clz, String name, DMLType dmlType) {
		super(factory, clz);
		setDmlType(dmlType);
		this.sessionFactory = factory;
		namedObject = sessionFactory.getQueryByNameAndClass(name, clz);
		fieldsValues = new TreeMap<>();
		sqlOperation = new SQLOperation() {
			@Override
			public long executeSQL(Connection conn) throws SQLException {
			    PreparedStatement stmt = null;
				try {
					setPreparedValues();
					sql = new StringBuilder();
					sql.append(namedObject.getSql());
	                stmt = conn.prepareStatement(sql.toString());
	                setPreparedStatementValue(stmt, dmlType);
	                showSql();
	                return stmt.executeUpdate();
				} finally {
				    closeStmt(stmt);
				}
			}
		};
	}

	/**
	 * 如果命名sql指定了查询的主表名，就使用sql指定的，否则使用entity注解声明的表名
	 * @return
	 */
	@Override
	protected String getCurrentTableName() {
		return getCurrentTableNameByNamedObject(namedObject);
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
	 * <b>@description  给变量设值</b>
	 * @param fieldAlias	变量别名
	 * @param value			变量的值
	 * @return
	 */
	public NamedUpdate<T> set(String fieldAlias, Object value){
	    List<Integer> indexes = namedObject.getFieldIndexes(fieldAlias);
	    for (int index : indexes) {
	    	fieldsValues.put(index, new PreparedValue(value));
	    }
	    return this;
	}
	
}

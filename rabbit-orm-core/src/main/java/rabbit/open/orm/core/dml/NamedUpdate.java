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

	protected NamedSQL nameObject;
	
	private TreeMap<Integer, PreparedValue> fieldsValues;
	
	public NamedUpdate(SessionFactory fatory, Class<T> clz, String name) {
		this(fatory, clz, name, DMLType.UPDATE);
	}

	public NamedUpdate(SessionFactory fatory, Class<T> clz, String name, DMLType dmlType) {
		super(fatory, clz);
		setDmlType(dmlType);
		this.sessionFactory = fatory;
		nameObject = (NamedSQL) sessionFactory.getQueryByNameAndClass(name, clz);
		fieldsValues = new TreeMap<>();
		sqlOperation = new SQLOperation() {
			@Override
			public long executeSQL(Connection conn) throws SQLException {
			    PreparedStatement stmt = null;
				try {
					setPreparedValues();
					sql = new StringBuilder();
					sql.append(nameObject.getSql());
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
	    List<Integer> indexes = nameObject.getFieldIndexes(fieldAlias);
	    for (int index : indexes) {
	    	fieldsValues.put(index, new PreparedValue(value));
	    }
	    return this;
	}
	
}

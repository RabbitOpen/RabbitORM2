package rabbit.open.orm.core.dml;

import java.sql.PreparedStatement;
import java.util.TreeMap;

import rabbit.open.orm.common.dml.DMLType;

/**
 * <b>@description 命名更新对象 </b>
 */
public class NamedUpdate<T> extends NonQueryAdapter<T> {
	
	public NamedUpdate(SessionFactory factory, Class<T> clz, String name) {
		this(factory, clz, name, DMLType.UPDATE);
	}

	protected NamedUpdate(SessionFactory factory, Class<T> clz, String name, DMLType dmlType) {
		super(factory, clz);
		setDmlType(dmlType);
		this.sessionFactory = factory;
		namedObject = sessionFactory.getQueryByNameAndClass(name, clz);
		fieldsValues = new TreeMap<>();
		sqlOperation = conn -> {
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
	 * <b>Description      单个设值</b>
	 * @param fieldAlias   字段在sql中的别名
	 * @param value        字段的值
	 * @param fieldName    字段在对应实体中的名字  	 
	 * @param entityClz    字段所属的实体   			 
	 * @return
	 */
	public NamedUpdate<T> set(String fieldAlias, Object value, String fieldName, Class<?> entityClz) {
	    setVariable(fieldAlias, value, fieldName, entityClz);
	    return this;
	}

	
}

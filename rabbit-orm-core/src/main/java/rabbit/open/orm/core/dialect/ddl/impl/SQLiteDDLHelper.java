package rabbit.open.orm.core.dialect.ddl.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.ddl.JoinTableDescriptor;
import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;

/**
 * <b>Description sqlite ddl助手</b>
 */
public class SQLiteDDLHelper extends OracleDDLHelper {

	private static final String PRIMARY_KEY = "PRIMARY KEY";

	public SQLiteDDLHelper() {
		typeStringCache.put(Date.class, TIMESTAMP);
		typeStringCache.put(String.class, VARCHAR);
		typeStringCache.put(BigDecimal.class, INTEGER);
		typeStringCache.put(Double.class, DOUBLE);
		typeStringCache.put(Float.class, FLOAT);
		typeStringCache.put(Integer.class, INTEGER);
		typeStringCache.put(Short.class, INTEGER);
		typeStringCache.put(Long.class, INTEGER);
	}

	@Override
	protected void dropTables(HashSet<String> entities) {
		if (entities.isEmpty()) {
			return;
		}
		dropEntityTables(entities);
		dropJoinTables(entities);
	}

	@Override
	protected HashSet<String> getExistedTables() {
		return getDDLHelperExistedTables();
	}

	@Override
	protected StringBuilder createTableSQL(Class<?> clz, String tableName) {
		Collection<FieldMetaData> fmds = MetaData.getCachedFieldsMetas(clz).values();
		StringBuilder sql = new StringBuilder("CREATE TABLE " + tableName.toUpperCase() + "(");
		appendFieldsSQLPiece(clz, fmds, sql);
		sql.deleteCharAt(sql.lastIndexOf(","));
		sql.append(")");
		return sql;
	}

	@Override
	protected String createSqlByPolicy(Policy policy) {
		if (Policy.UUID.equals(policy)) {
			return PRIMARY_KEY;
		} else if (Policy.AUTOINCREMENT.equals(policy)) {
			return PRIMARY_KEY + getAutoIncrement();
		} else {
			return "";
		}
	}

	@Override
	protected StringBuilder createJoinTableSql(String tb, List<JoinTableDescriptor> list) {
		StringBuilder sql = new StringBuilder("CREATE TABLE " + tb.toUpperCase() + "(");
		for (JoinTableDescriptor jtd : list) {
			sql.append(getColumnName(jtd.getColumnName()) + " ");
			sql.append(getSqlTypeByJavaType(jtd.getType(), jtd.getColumnLength()));
			if (null != jtd.getPolicy()) {
				if (jtd.getPolicy().equals(Policy.AUTOINCREMENT)) {
					sql.append(" " + PRIMARY_KEY + getAutoIncrement() + ", ");
				}
			} else {
				sql.append(", ");
			}
		}
		sql.deleteCharAt(sql.lastIndexOf(","));
		sql.append(")");
		return sql;
	}

	@Override
	protected String getAutoIncrement() {
		return " AUTOINCREMENT ";
	}

	@Override
	protected String getAddColumnKeywords() {
		return " ADD COLUMN ";
	}

	@Override
	public String getColumnName(Column column) {
		if (column.keyWord()) {
			return "\"" + column.value().trim().toUpperCase() + "\"";
		}
		return column.value().trim();
	}

	@Override
	protected List<StringBuilder> getCommentSqls() {
		return new ArrayList<>();
	}

}

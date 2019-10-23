package rabbit.open.orm.core.dialect.ddl.impl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import rabbit.open.orm.common.ddl.JoinTableDescriptor;
import rabbit.open.orm.common.exception.RabbitDDLException;

/**
 * <b>Description: sqlserver ddl助手</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
public class SQLServerDDLHelper extends OracleDDLHelper {

	public SQLServerDDLHelper() {
		typeStringCache.put(Date.class, DATETIME);
		typeStringCache.put(String.class, VARCHAR);
		typeStringCache.put(BigDecimal.class, BIGINT);
		typeStringCache.put(Double.class, FLOAT);
		typeStringCache.put(Float.class, FLOAT);
		typeStringCache.put(Integer.class, BIGINT);
		typeStringCache.put(Short.class, BIGINT);
		typeStringCache.put(Long.class, BIGINT);
	}

	@Override
	protected Set<String> getExistedTables() {
		try {
			return readTablesFromDB(getConnection());
		} catch (SQLException e) {
			throw new RabbitDDLException(e);
		}
	}

	/**
	 * 
	 * <b>Description: 查询有外键的表信息 </b><br>
	 * @return
	 * 
	 */
	@Override
	protected String getForeignKeyTableSql() {
		return "SELECT OBJECT_NAME(PARENT_OBJECT_ID) AS TABLE_NAME, NAME AS FK_NAME FROM SYS.OBJECTS WHERE TYPE='F'";
	}

	@Override
	protected StringBuilder createJoinTableSql(String tb, List<JoinTableDescriptor> list) {
		return callSuperCreateJoinTableSql(tb, list);
	}

	@Override
	protected String getAutoIncrement() {
		return " IDENTITY(1,1) ";
	}
	
	@Override
	protected void generateComment(StringBuilder sql, String comment, String tableName, String columnName) {
		if (null != comment && !"".equals(comment.trim())) {
			comments.add(new StringBuilder("exec sp_addextendedproperty N'MS_Description', N'" + comment 
					+ "', N'user', N'dbo', N'table', N'" + tableName + "', N'column', N'" + columnName + "'"));
		}
	}

}

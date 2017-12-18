package rabbit.open.orm.dialect.ddl.impl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import rabbit.open.orm.ddl.JoinTableDescriptor;
import rabbit.open.orm.dialect.ddl.DDLHelper;
import rabbit.open.orm.dml.util.SQLFormater;
import rabbit.open.orm.exception.RabbitDDLException;

/**
 * <b>Description: 	mysql ddl助手</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public class MySQLDDLHelper extends DDLHelper{

	private static final String SET_FOREIGN_KEY_CHECKS_1 = "SET FOREIGN_KEY_CHECKS = 1";
	
    private static final String SET_FOREIGN_KEY_CHECKS_0 = "SET FOREIGN_KEY_CHECKS = 0";

    /**
	 * 
	 * <b>Description:	删除指定表</b><br>
	 * @param entities	实体class的集合
	 * 
	 */
	@Override
	protected void dropTables(HashSet<String> entities) {
		if(entities.isEmpty()){
			return;
		}
		dropEntityTables(entities);
		dropJoinTables(entities);
	}

	private void dropJoinTables(HashSet<String> entities) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.addBatch(SET_FOREIGN_KEY_CHECKS_0);
			HashMap<String, List<JoinTableDescriptor>> joinTables = getJoinTables(entities);
			for(String table : joinTables.keySet()){
				if(!isTableExists(getExistedTables(), table)){
					continue;
				}
				String drop = "drop table " + table ;
				logger.info(SQLFormater.format(drop).toUpperCase());
				stmt.addBatch(drop);
			}
			stmt.addBatch(SET_FOREIGN_KEY_CHECKS_1);
			stmt.executeBatch();
		} catch (Exception e) {
			throw new RabbitDDLException(e);
		} finally {
			closeStmt(stmt);
		}
	}

	/**
	 * 
	 * <b>Description:	删除实体类对应的表</b><br>
	 * @param entities	
	 * 
	 */
	private void dropEntityTables(HashSet<String> entities) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.addBatch(SET_FOREIGN_KEY_CHECKS_0);
			for(String name : entities){
				String drop = createDropSqlByClass(name);
				if(null == drop){
					continue;
				}
				logger.info(SQLFormater.format(drop).toUpperCase());
				stmt.addBatch(drop);
			}
			stmt.addBatch(SET_FOREIGN_KEY_CHECKS_1);
			stmt.executeBatch();
		} catch (SQLException e) {
			throw new RabbitDDLException(e);
		} finally {
			closeStmt(stmt);
		}
	}
	
	protected void createJoinTables(HashSet<String> entities) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.addBatch(SET_FOREIGN_KEY_CHECKS_0);
			HashMap<String, List<JoinTableDescriptor>> joinTables = getJoinTables(entities);
			for(Entry<String, List<JoinTableDescriptor>> entry : joinTables.entrySet()){
				if(isTableExists(getExistedTables(), entry.getKey())){
					continue;
				}
				StringBuilder sql = createJoinTableSql(entry.getKey(), entry.getValue());
				logger.info(SQLFormater.format(sql.toString()).toUpperCase());
				stmt.addBatch(sql.toString());
			}
			stmt.addBatch(SET_FOREIGN_KEY_CHECKS_1);
			stmt.executeBatch();
		} catch (Exception e) {
			throw new RabbitDDLException(e);
		} finally {
			closeStmt(stmt);
		}
	}

	/**
	 * 
	 * <b>Description:	实体表</b><br>
	 * @param entities	
	 * 
	 */
	@Override
	protected void createEntityTables(HashSet<String> entities) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.addBatch(SET_FOREIGN_KEY_CHECKS_0);
			for(String name : entities){
				StringBuilder sql = createSqlByClass(name);
				if(null == sql){
					continue;
				}
				logger.info(SQLFormater.format(sql.toString()).toUpperCase());
				stmt.addBatch(sql.toString());
			}
			stmt.addBatch(SET_FOREIGN_KEY_CHECKS_1);
			stmt.executeBatch();
		} catch (SQLException e) {
			throw new RabbitDDLException(e);
		} finally {
			closeStmt(stmt);
		}
	}


	/**
	 * 
	 * <b>Description:	根据java类型转sql类型</b><br>
	 * @param type
	 * @param length
	 * @return	
	 * 
	 */
	protected String getSqlTypeByJavaType(Class<?> type, int length) {
		if(type.equals(Date.class)){
			return getDateType();
		}
		if(type.equals(String.class)){
			return getVarcharType() + "(" + length + ")";
		}
		if(type.equals(Integer.class) || type.equals(Short.class) || type.equals(Long.class)){
			return "BIGINT ";
		}
		if(type.equals(Float.class)){
			return "FLOAT ";
		}
		if(type.equals(Double.class)){
			return "DOUBLE ";
		}
		if(type.equals(BigDecimal.class)){
		    return "BIGINT ";
		}
		throw new RabbitDDLException("unsupported java type[" + type.getName() + "] is found!");
	}
	
	@Override
	protected String getVarcharType() {
		return "VARCHAR";
	}

	@Override
	protected String getDateType() {
		return "DATETIME";
	}
	
	@Override
	protected String getAutoIncrement() {
		return "AUTO_INCREMENT";
	}
	
	@Override
	protected String getAddColumnKeywords() {
		return " ADD COLUMN ";
	}

    @Override
    protected String getColumnName(String realName) {
        return realName;
    }
	
	
}

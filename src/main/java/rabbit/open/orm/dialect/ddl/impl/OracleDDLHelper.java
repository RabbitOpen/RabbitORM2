package rabbit.open.orm.dialect.ddl.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import rabbit.open.orm.ddl.JoinTableDescriptor;
import rabbit.open.orm.dialect.ddl.DDLHelper;
import rabbit.open.orm.dml.util.SQLFormater;
import rabbit.open.orm.exception.RabbitDDLException;

/**
 * <b>Description: 	oracle ddl助手</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public class OracleDDLHelper extends DDLHelper{

	private static final String GET_FOREIGN_KEY_TABLE_SQL = "SELECT TABLE_NAME, A.CONSTRAINT_NAME AS FK_NAME FROM USER_CONSTRAINTS A WHERE A.CONSTRAINT_TYPE = 'R'";

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
		removeForeignKeys();
		dropEntityTables(entities);
		dropJoinTables(entities);
	}

	@Override
	protected HashSet<String> getExistedTables() {
	    Statement stmt = null;
	    ResultSet tables = null;
        try {
            stmt = conn.createStatement();
            tables = stmt.executeQuery("SELECT * FROM ALL_TABLES WHERE OWNER IN(SELECT USERNAME FROM USER_USERS)");
            HashSet<String> existsTables = new HashSet<>();
            while(tables.next()){
                existsTables.add(tables.getString("TABLE_NAME"));
            }
            return existsTables;
        } catch (SQLException e) {
            throw new RabbitDDLException(e);
        } finally {
            closeResultSet(tables);
            closeStmt(stmt);
        }
    }

    private void closeResultSet(ResultSet tables) {
        if(null == tables){
            return;
        }
        try {
            tables.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }
	
    /**
     * 
     * <b>Description:  清除oracle表中外键</b><br>.	
     * 
     */
    private void removeForeignKeys() {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(getForeignKeyTableSql());
            List<String> fks = new ArrayList<>();
            while(rs.next()){
                String tbName = rs.getString("TABLE_NAME");
                String fkName = rs.getString("FK_NAME");
                fks.add(tbName + "," + fkName);
            }
            rs.close();
            for(String d : fks){
                StringBuilder sql = new StringBuilder("ALTER TABLE ").append(d.split(",")[0])
                        .append(" DROP CONSTRAINT ").append(d.split(",")[1]);
                logger.info(sql);
                stmt.executeUpdate(sql.toString());
            }
        } catch (Exception e) {
            throw new RabbitDDLException(e);
        } finally {
            closeResultSet(rs);
            closeStmt(stmt);
        }
    }
    
    protected StringBuilder callSuperCreateJoinTableSql(String tb, List<JoinTableDescriptor> list) {
        return super.createJoinTableSql(tb, list);
    }

    /**
     * 
     * <b>Description:  创建中间表sql</b><br>
     * @param tb
     * @param list
     * @return
     * 
     */
    @Override
    protected StringBuilder createJoinTableSql(String tb, List<JoinTableDescriptor> list) {
        StringBuilder sql = new StringBuilder("CREATE TABLE " + tb.toUpperCase() + "(");
        String pkName = "";
        for(JoinTableDescriptor jtd : list){
            sql.append(getColumnName(jtd.getColumnName()) + " ");
            sql.append(getSqlTypeByJavaType(jtd.getType(), jtd.getColumnLength()));
            if(null != jtd.getPolicy()){
                sql.append(" NOT NULL, ");
                pkName = jtd.getColumnName();
            }else{
                sql.append(", ");
            }
        }
        if(!"".equals(pkName)){
            sql.append("PRIMARY KEY(" + getColumnName(pkName) + "),");
        }
        sql.deleteCharAt(sql.lastIndexOf(","));
        sql.append(")");
        return sql;
    }
    
    /**
     * 
     * <b>Description: 查询有外键的表信息 </b><br>.
     * @return	
     * 
     */
    protected String getForeignKeyTableSql() {
        return GET_FOREIGN_KEY_TABLE_SQL;
    }

	private void dropJoinTables(HashSet<String> entities) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			HashMap<String, List<JoinTableDescriptor>> joinTables = getJoinTables(entities);
			for(String table : joinTables.keySet()){
				if(!isTableExists(getExistedTables(), table)){
					continue;
				}
				String drop = "drop table " + table;
				logger.info(SQLFormater.format(drop).toUpperCase());
				stmt.execute(drop);
			}
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
			for(String name : entities){
				String drop = createDropSqlByClass(name);
				if(null == drop){
					continue;
				}
				logger.info(SQLFormater.format(drop).toUpperCase());
				stmt.execute(drop);
			}
		} catch (SQLException e) {
			throw new RabbitDDLException(e);
		} finally {
			closeStmt(stmt);
		}
	}
	
	@Override
	protected void createJoinTables(HashSet<String> entities) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			HashMap<String, List<JoinTableDescriptor>> joinTables = getJoinTables(entities);
			for(Entry<String, List<JoinTableDescriptor>> entry : joinTables.entrySet()){
				if(isTableExists(getExistedTables(), entry.getKey())){
					continue;
				}
				StringBuilder sql = createJoinTableSql(entry.getKey(), entry.getValue());
				logger.info(SQLFormater.format(sql.toString()).toUpperCase());
				stmt.execute(sql.toString());
			}
		} catch (Exception e) {
			throw new RabbitDDLException(e);
		} finally {
			closeStmt(stmt);
		}
	}

	@Override
	protected void createEntityTables(HashSet<String> entities) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			for(String name : entities){
				StringBuilder sql = createSqlByClass(name);
				if(null == sql){
					continue;
				}
				logger.info(SQLFormater.format(sql.toString()).toUpperCase());
				stmt.execute(sql.toString());
			}
		} catch (SQLException e) {
			throw new RabbitDDLException(e);
		} finally {
			closeStmt(stmt);
		}
	}

	@Override
    public String getNumberType() {
	    return "NUMBER(20, 0) ";
    }

	@Override
    public String getFloatType() {
	    return "NUMBER(8,5) ";
    }

    @Override
    public String getDoubleType() {
        return "NUMBER(8,5) ";
    }

    @Override
    public String getBigDecimalType() {
        return "NUMBER(20, 0) ";
    }
	
	@Override
	protected String getVarcharType() {
		return "VARCHAR2";
	}

	@Override
	protected String getDateType() {
		return "DATE";
	}
	
	@Override
	protected String getAutoIncrement() {
		return "AUTO_INCREMENT";
	}
	
	@Override
	protected String getAddColumnKeywords() {
		return " ADD ";
	}

    @Override
    protected String getColumnName(String realName) {
        return  realName;
    }
	
	
}

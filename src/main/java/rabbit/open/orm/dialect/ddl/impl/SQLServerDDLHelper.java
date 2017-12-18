package rabbit.open.orm.dialect.ddl.impl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import rabbit.open.orm.ddl.JoinTableDescriptor;
import rabbit.open.orm.dml.policy.Policy;
import rabbit.open.orm.exception.RabbitDDLException;

/**
 * <b>Description: 	sqlserver ddl助手</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public class SQLServerDDLHelper extends OracleDDLHelper{

	@Override
	protected HashSet<String> getExistedTables() {
	    try {
            return readTablesFromDB();
        } catch (SQLException e) {
            throw new RabbitDDLException(e);
        }
    }

	/**
     * 
     * <b>Description: 查询有外键的表信息 </b><br>.
     * @return  
     * 
     */
    @Override
    protected String getForeignKeyTableSql() {
        return "SELECT OBJECT_NAME(PARENT_OBJECT_ID) AS TABLE_NAME, NAME AS FK_NAME FROM SYS.OBJECTS WHERE TYPE='F'";
    }
    
    @Override
    protected StringBuilder createJoinTableSql(String tb,
            List<JoinTableDescriptor> list) {
        StringBuilder sql = new StringBuilder("CREATE TABLE " + tb.toUpperCase() + "(");
        String pkName = "";
        for(JoinTableDescriptor jtd : list){
            sql.append(getColumnName(jtd.getColumnName()) + " ");
            sql.append(getSqlTypeByJavaType(jtd.getType(), jtd.getColumnLength()));
            if(null != jtd.getPolicy()){
                if(jtd.getPolicy().equals(Policy.AUTOINCREMENT)){
                    sql.append(getAutoIncrement() + ", ");
                }else{
                    sql.append(" NOT NULL, ");
                }
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
	 * <b>Description:	根据java类型转sql类型</b><br>
	 * @param type
	 * @param length
	 * @return	
	 * 
	 */
    @Override
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
			return "FLOAT ";
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
		return "IDENTITY(1,1) ";
	}
	
}

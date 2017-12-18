package rabbit.open.orm.dialect.ddl.impl;

import java.math.BigDecimal;
import java.util.Date;

import rabbit.open.orm.exception.RabbitDDLException;

/**
 * <b>Description: 	db2 ddl助手</b><br>
 *                  暂时不支持新增主键字段
 * <b>@author</b>	肖乾斌
 * 
 */
public class DB2DDLHelper extends SQLServerDDLHelper{

	/**
     * 
     * <b>Description: 查询有外键的表信息 </b><br>.
     * @return  
     * 
     */
    @Override
    protected String getForeignKeyTableSql() {
        return "SELECT CONSTNAME AS FK_NAME, TABNAME AS TABLE_NAME FROM SYSCAT.REFERENCES";
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
			return "DOUBLE ";
		}
		if(type.equals(BigDecimal.class)){
            return "BIGINT ";
        }
		throw new RabbitDDLException("unsupported java type[" + type.getName() + "] is found!");
	}
	

	@Override
	protected String getDateType() {
		return "TIMESTAMP";
	}
	
	@Override
	protected String getAutoIncrement() {
		return " GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1 )";
	}
	
}

package rabbit.open.orm.dialect.ddl.impl;

import java.math.BigDecimal;
import java.util.Date;


/**
 * <b>Description: 	db2 ddl助手</b><br>
 *                  暂时不支持新增主键字段
 * <b>@author</b>	肖乾斌
 * 
 */
public class DB2DDLHelper extends SQLServerDDLHelper{

    public DB2DDLHelper() {
        typeStringCache.put(Date.class, TIMESTAMP);
        typeStringCache.put(String.class, VARCHAR);
        typeStringCache.put(BigDecimal.class, BIGINT);
        typeStringCache.put(Double.class, DOUBLE);
        typeStringCache.put(Float.class, FLOAT);
        typeStringCache.put(Integer.class, BIGINT);
        typeStringCache.put(Short.class, BIGINT);
        typeStringCache.put(Long.class, BIGINT);
    }
    
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
    
	@Override
	protected String getAutoIncrement() {
		return " GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1 )";
	}
	
}

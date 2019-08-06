package rabbit.open.orm.dialect.ddl.impl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import rabbit.open.orm.ddl.JoinTableDescriptor;
import rabbit.open.orm.exception.RabbitDDLException;

/**
 * <b>Description: 	sqlserver ddl助手</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public class SQLServerDDLHelper extends OracleDDLHelper{

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
    protected StringBuilder createJoinTableSql(String tb, List<JoinTableDescriptor> list) {
        return callSuperCreateJoinTableSql(tb, list);
    }
    
	@Override
	protected String getAutoIncrement() {
		return " IDENTITY(1,1) ";
	}
	
}

package rabbit.open.orm.dialect.ddl.impl;

import java.sql.SQLException;
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
    public String getNumberType() {
        return "BIGINT ";
    }

    @Override
    public String getFloatType() {
        return "FLOAT ";
    }

    @Override
    public String getDoubleType() {
        return "FLOAT ";
    }

    @Override
    public String getBigDecimalType() {
        return "BIGINT ";
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

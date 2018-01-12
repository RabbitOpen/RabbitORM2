package rabbit.open.orm.dialect.ddl.impl;


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
    
    @Override
    public String getDoubleType() {
        return "DOUBLE ";
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

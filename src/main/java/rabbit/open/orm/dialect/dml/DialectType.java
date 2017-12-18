package rabbit.open.orm.dialect.dml;

import rabbit.open.orm.exception.RabbitDMLException;

/**
 * <b>Description: 	方言类型</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public enum DialectType {

	MYSQL, ORACLE, SQLSERVER, DB2;
	
	public static DialectType format(String name){
		for(DialectType dt : DialectType.values()){
			if(name.equalsIgnoreCase(dt.name())){
				return dt;
			}
		}
		throw new RabbitDMLException("unkown dialect[" + name + "] is found!");
	}
	
	public boolean isOracle(){
		return name().equals(ORACLE.name());
	}

	public boolean isMysql(){
		return name().equals(MYSQL.name());
	}

	public boolean isSQLServer(){
		return name().equals(SQLSERVER.name());
	}

	public boolean isDB2(){
		return name().equals(DB2.name());
	}
}

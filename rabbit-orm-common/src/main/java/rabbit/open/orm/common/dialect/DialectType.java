package rabbit.open.orm.common.dialect;

import rabbit.open.orm.common.exception.RabbitDMLException;

/**
 * <b>Description: 方言类型</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
public enum DialectType {

	MYSQL, ORACLE, SQLSERVER, DB2, SQLITE3;

	public static DialectType format(String name) {
		for (DialectType dt : DialectType.values()) {
			if (name.equalsIgnoreCase(dt.name())) {
				return dt;
			}
		}
		throw new RabbitDMLException("unknown dialect[" + name + "] is found!");
	}

	public boolean isOracle() {
		return name().equals(ORACLE.name());
	}

	public boolean isMysql() {
		return name().equals(MYSQL.name());
	}

	public boolean isSQLServer() {
		return name().equals(SQLSERVER.name());
	}

	public boolean isDB2() {
		return name().equals(DB2.name());
	}

	public boolean isSQLITE3() {
		return name().equals(SQLITE3.name());
	}
}

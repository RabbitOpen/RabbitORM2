package rabbit.open.orm.common.dml;

/**
 * 
 * id策略
 * 
 * @author 肖乾斌
 * 
 */
public enum Policy {

	UUID("UUID策略"), SEQUENCE("ORACLE 序列"), AUTOINCREMENT("自增长, 兼容mysql, sqlserver, DB2"), NONE("没有策略");

	Policy(String desc) {
	}
}

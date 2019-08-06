package rabbit.open.orm.core.dml.meta;

/**
 * 预编译sql描述符
 * @author	肖乾斌
 * 
 */
public class PreparedSqlDescriptor {

	//sql
	private StringBuilder sql;
	
	//执行次数
	private int executeTimes = 1;

	public PreparedSqlDescriptor(int executeTimes) {
		super();
		this.executeTimes = executeTimes;
	}

	public void setSql(StringBuilder sql) {
		this.sql = sql;
	}
	
	public StringBuilder getSql() {
        return sql;
    }

    public int getExecuteTimes() {
        return executeTimes;
    }

	
}

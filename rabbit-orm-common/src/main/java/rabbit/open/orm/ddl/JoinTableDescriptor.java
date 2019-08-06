package rabbit.open.orm.ddl;

import rabbit.open.orm.dml.policy.Policy;

/**
 * 
 * 中间表描述符号
 * @author 肖乾斌
 *
 */
public class JoinTableDescriptor {
	
	private Class<?> type;
	
	private String columnName;
	
	private Policy policy;
	
	private int columnLength = 0;
	
	public JoinTableDescriptor(Class<?> type, String columnName, Policy policy, int columnLength) {
        super();
        this.type = type;
        this.columnName = columnName;
        this.policy = policy;
        this.columnLength = columnLength;
    }

    public JoinTableDescriptor(Class<?> type, String columnName, int columnLength) {
        super();
        this.type = type;
        this.columnName = columnName;
        this.columnLength = columnLength;
    }

	public String getColumnName() {
		return columnName;
	}

	public Class<?> getType() {
		return type;
	}

	public Policy getPolicy() {
		return policy;
	}

	public int getColumnLength() {
		return columnLength;
	}
	
}

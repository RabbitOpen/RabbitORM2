package rabbit.open.orm.codegen;

/**
 * <b>@description DB 字段描述符 </b>
 */
public class DBFieldDescriptor {

	// java类型
	private Class<?> javaType;
	
	// 长度敏感
	private boolean lengthSensitive;
	
	public DBFieldDescriptor(Class<?> javaType, boolean lengthSensitive) {
		super();
		this.javaType = javaType;
		this.lengthSensitive = lengthSensitive;
	}

	public Class<?> getJavaType() {
		return javaType;
	}

	public boolean isLengthSensitive() {
		return lengthSensitive;
	}

}

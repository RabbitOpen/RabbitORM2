package rabbit.open.codegen;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <b>@description 数据库字段和java字段的映射关系 </b>
 */
public class MappingRegistry {

	private static Map<String, DBFieldDescriptor> registry = new HashMap<>();
	
	private MappingRegistry() {}
	
	static {
		// 字符串类型
		registry.put("VARCHAR", new DBFieldDescriptor(String.class, true));
		registry.put("VARCHAR2", new DBFieldDescriptor(String.class, true));
		registry.put("CHAR", new DBFieldDescriptor(String.class, true));
		registry.put("MEDIUMTEXT", new DBFieldDescriptor(String.class, true));
		// 日期类型
		registry.put("DATETIME", new DBFieldDescriptor(Date.class, false));
		registry.put("TIMESTAMP", new DBFieldDescriptor(Date.class, false));
		// 数据类型
		registry.put("INT", new DBFieldDescriptor(Integer.class, false));
		registry.put("SMALLINT", new DBFieldDescriptor(Integer.class, false));
		registry.put("BIGINT", new DBFieldDescriptor(Integer.class, false));
		registry.put("TINYINT", new DBFieldDescriptor(Integer.class, false));
		registry.put("FLOAT", new DBFieldDescriptor(Float.class, false));
		registry.put("DOUBLE", new DBFieldDescriptor(Double.class, false));
		registry.put("BIT", new DBFieldDescriptor(Boolean.class, false));
	}
	
	public static void regist(String key, DBFieldDescriptor value) {
		registry.put(key, value);
	}
	
	public static DBFieldDescriptor getFieldDescriptor(String key) {
		return registry.get(key);
	}
}

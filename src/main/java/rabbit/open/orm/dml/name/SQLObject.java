package rabbit.open.orm.dml.name;

import java.util.HashMap;
import java.util.Map;

import rabbit.open.orm.exception.RabbitDMLException;


/**
 * <b>Description  sql对象</b>
 */
public class SQLObject {

	protected String sql;
	
	protected String name;
	
	protected Map<String, Integer> fieldsMapping = new HashMap<>();
	
	public SQLObject(String sql, String name) {
		super();
		this.sql = sql.trim();
		this.name = name;
	}

	public String getSql() {
		return sql;
	}
	
	protected boolean isEmpty(String str){
	    return null == str || "".equals(str.trim());
	}
	
	/**
	 * 
	 * <b>Description:  根据字段名查找数序号</b><br>.
	 * @param fieldName
	 * @return	
	 * 
	 */
	public int getFieldIndex(String fieldName){
	    if(!fieldsMapping.containsKey(fieldName)){
	        throw new RabbitDMLException("unkown field[" + fieldName + "] is found!");
	    }
	    return fieldsMapping.get(fieldName);
	}
}

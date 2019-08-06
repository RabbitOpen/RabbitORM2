package rabbit.open.orm.dml.name;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import rabbit.open.orm.exception.UnKnownFieldException;


/**
 * <b>Description  sql对象</b>
 */
public class SQLObject {

	protected String sql;
	
	protected String name;
	
	protected Map<Integer, String> fieldsMapping = new HashMap<>();
	
	public SQLObject(String sql, String name) {
		super();
		this.sql = sql.trim();
		this.name = name;
	}

	public String getSql() {
		return sql;
	}
	
	/**
	 * 
	 * <b>Description:  根据字段名查找数序号</b><br>.
	 * @param fieldName
	 * @return	
	 * 
	 */
	public List<Integer> getFieldIndexes(String fieldName) {
	    List<Integer> indexes = new ArrayList<>();
        if (!fieldsMapping.containsValue(fieldName)) {
	        throw new UnKnownFieldException("field[" + fieldName + "] is not existed in query[" + name + "]!");
	    }
        for (Entry<Integer, String> entry : fieldsMapping.entrySet()) {
            if (entry.getValue().equals(fieldName)) {
                indexes.add(entry.getKey());
            }
        }
	    return indexes;
	}
}

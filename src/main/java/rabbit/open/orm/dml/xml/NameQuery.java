package rabbit.open.orm.dml.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rabbit.open.orm.exception.RabbitDMLException;


public class NameQuery {

    //关键字的正则表达式
    private static final String REPLACE_WORD = "\\$\\{(.*?)\\}";
    
	private String sql;
	
	//jdbc/select
	private String type;	
	
	private String queryName;
	
	private Map<String, Integer> fieldsMapping = new HashMap<>();
	
	public NameQuery(String sql, String type, String queryName) {
		super();
		this.sql = sql.trim();
		this.type = type;
		this.queryName = queryName;
		if(!isNameQuery()){
		    return;
		}
		analyseNameQuery(sql);
	}

    /**
     * 
     * <b>Description:  分析命名查询sql</b><br>.
     * @param sql	
     * 
     */
    private void analyseNameQuery(String sql) {
        Pattern pattern  = Pattern.compile(REPLACE_WORD);
		Matcher matcher = pattern.matcher(sql);
		int index = 0;
        while(matcher.find()){
            if(isEmpty(matcher.group(1))){
                throw new RabbitDMLException("empty field is found in NameQuery[" + this.queryName + "]");
            }
            fieldsMapping.put(matcher.group(1).trim(), index++);
            this.sql = this.sql.replace(matcher.group(0), "?");
		}
    }

	public String getSql() {
		return sql;
	}
	
	public boolean isNameQuery(){
		return "select".equalsIgnoreCase(type);
	}
	
	private boolean isEmpty(String str){
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

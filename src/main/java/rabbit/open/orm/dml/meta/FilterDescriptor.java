package rabbit.open.orm.dml.meta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import rabbit.open.orm.annotation.Relation.FilterType;

/**
 * 过滤条件描述符
 * @author	肖乾斌
 * 
 */
public class FilterDescriptor {

	private String key;
	
	private Object value;
	
	private String filter;
	
	private String connector = " AND ";
	
	//两表关联时的外键表
	private String filterTable;

	//联合查询时的依赖
	private Class<?> joinDependency;
	
	//在父类表中的字段
	private Field joinField;
	
	//在父类表中的字段
	private List<Field> joinFields;
	
	public Field getJoinField() {
		return joinField;
	}

	public List<Field> getJoinFields() {
	    return joinFields;
	}

	public void setJoinField(Field joinField) {
		this.joinField = joinField;
		addJoinField(this.joinField);
	}
	
    public void addJoinField(Field joinField) {
        for (Field f : this.joinFields) {
            if (f.equals(joinField)) {
                return;
            }
        }
        joinFields.add(joinField);
    }

	//表示该filter是否是表之间关联的过滤条件
	private boolean isJoinOn = false;

	public boolean isJoinOn() {
		return isJoinOn;
	}

	public void setJoinOn(boolean isJoinOn) {
		this.isJoinOn = isJoinOn;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getValue() {
		return value;
	}

	public String getFilter() {
		return filter;
	}

	public FilterDescriptor(String key, Object value, String filter) {
		super();
		this.key = key;
		this.value = value;
		this.filter = filter;
		joinFields = new ArrayList<>();
	}

	public FilterDescriptor(String key, Object value) {
		this(key, value, FilterType.EQUAL.value());
	}

	public boolean isEqual(FilterDescriptor fd){
		return this.key.equals(fd.getKey()) 
			&& ((null == this.filter && null == fd.getFilter()) || (null != this.filter && null != fd.getFilter() && this.filter.equals(fd.getFilter())))
			&& ((null == this.value && null == fd.getValue()) || (null != this.value && null != fd.getValue() && this.value.equals(fd.getValue())));
	}

	public String getFilterTable() {
		return filterTable;
	}

	public void setFilterTable(String filterTable) {
		this.filterTable = filterTable;
	}

	public Class<?> getJoinDependency() {
		return joinDependency;
	}

	public void setJoinDependency(Class<?> joinDependency) {
		this.joinDependency = joinDependency;
	}

	public String getConnector() {
		return connector;
	}
	
}

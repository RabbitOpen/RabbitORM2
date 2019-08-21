package rabbit.open.orm.core.dml.meta;

import java.lang.reflect.Field;

import rabbit.open.orm.common.dml.FilterType;

/**
 * 过滤条件描述符
 * 
 * @author 肖乾斌
 * 
 */
public class FilterDescriptor {

    private String key;

    private Object value;

    private String filter;

    private String connector = " AND ";

    // 两表关联时的外键表
    private String filterTable;

    // 联合查询时的依赖
    private Class<?> joinDependency;

    // 在父类表中的字段
    private Field joinField;

    // 过滤条件对应的字段
    private Field field;

    // 同类型字段下标
    private int index = 0;

    // 表示该filter是否是表之间关联的过滤条件
    private boolean isJoinOn = false;

    // 标识是否是拥有同类型字段
    private boolean multiFetchField = false;
    
    public FilterDescriptor(String key, Object value, String filter) {
        super();
        this.key = key;
        this.value = value;
        this.filter = filter;
    }

    public FilterDescriptor(String key, Object value) {
        this(key, value, FilterType.EQUAL.value());
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isMultiFetchField() {
        return multiFetchField;
    }

    public void setMultiFetchField(boolean multiFetchField) {
        this.multiFetchField = multiFetchField;
    }

    public Field getJoinField() {
        return joinField;
    }

    public void setJoinField(Field joinField) {
        this.joinField = joinField;
    }

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
    
	public boolean isEqual(FilterDescriptor fd) {
        return this.key.equals(fd.getKey()) 
            && ((null == filter && null == fd.getFilter()) || (null != filter && null != fd.getFilter() && filter.equals(fd.getFilter())))
            && ((null == value && null == fd.getValue()) || (null != value && null != fd.getValue() && value.equals(fd.getValue())))
            && joinField.equals(fd.getJoinField());
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

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

}

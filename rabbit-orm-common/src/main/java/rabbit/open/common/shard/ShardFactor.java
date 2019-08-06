package rabbit.open.common.shard;

import java.lang.reflect.Field;

/**
 * <b>Description  切片因子</b>
 */
public class ShardFactor {

    //字段
    private Field field;
    
    //过滤条件
    private String filter;
    
    //字段的值
    private Object value;

    public ShardFactor(Field field, String filter, Object value) {
        super();
        this.field = field;
        this.filter = filter.trim();
        this.value = value;
    }

    public Field getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }
    
    public String getFilter() {
        return filter;
    }
    
}

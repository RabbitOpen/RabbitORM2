package rabbit.open.orm.shard;

import java.lang.reflect.Field;

/**
 * <b>Description  切片因子</b>
 */
public class ShardFactor {

    //字段
    private Field field;
    
    //字段的值
    private Object value;

    public ShardFactor(Field field, Object value) {
        super();
        this.field = field;
        this.value = value;
    }

    public Field getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }
    
}

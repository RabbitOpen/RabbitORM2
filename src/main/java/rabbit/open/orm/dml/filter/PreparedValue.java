package rabbit.open.orm.dml.filter;

import java.lang.reflect.Field;

public class PreparedValue {

    private Object value;
    
    private Field field;

    public Object getValue() {
        return value;
    }

    public Field getField() {
        return field;
    }

    public PreparedValue(Object value, Field field) {
        super();
        this.value = value;
        this.field = field;
    }

    public PreparedValue(Object value) {
        super();
        this.value = value;
    }
    
}

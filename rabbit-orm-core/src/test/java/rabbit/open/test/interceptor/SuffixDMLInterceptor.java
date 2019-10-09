package rabbit.open.test.interceptor;

import java.lang.reflect.Field;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.core.dml.interceptor.DMLInterceptor;
import rabbit.open.test.entity.User;

public class SuffixDMLInterceptor implements DMLInterceptor {

    public static String suffix = "@@encrypt";

    @Override
    public Object onValueSet(Object value, Field field, DMLType type) {
        if (field.getDeclaringClass().equals(User.class)
        && field.getName().equals("name") && null != value) {
            return value + suffix;
        }
        return value;
    }

    @Override
    public Object onValueGot(Object value, Field field) {
        if (isTargetField(value, field)) {
            return value.toString().replaceAll(suffix, "");
        }
        return value;
    }

    private boolean isTargetField(Object value, Field field) {
        return (field.getDeclaringClass().equals(User.class)
        && field.getName().equals("name")) && null != value;
    }

}

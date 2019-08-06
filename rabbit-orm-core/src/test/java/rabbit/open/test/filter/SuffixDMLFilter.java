package rabbit.open.test.filter;

import java.lang.reflect.Field;

import rabbit.open.common.dml.DMLType;
import rabbit.open.orm.dml.filter.DMLFilter;
import rabbit.open.test.entity.User;

public class SuffixDMLFilter implements DMLFilter {

    public static String suffix = "@@encrypt";

    @Override
    public Object onValueSetted(Object value, Field field, DMLType type) {
        if (field.getDeclaringClass().equals(User.class)
        && field.getName().equals("name") && null != value) {
            return value + suffix;
        }
        return value;
    }

    @Override
    public Object onValueGetted(Object value, Field field) {
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

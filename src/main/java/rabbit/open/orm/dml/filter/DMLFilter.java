package rabbit.open.orm.dml.filter;

import java.lang.reflect.Field;

/**
 * <b>Description  dml操作前置过滤器</b>
 */
public interface DMLFilter {

    /**
     * <b>Description   jdbc存储过程设值前置事件</b>
     * @param value     真实的值
     * @param field     对应的字段
     * @param type      dml操作类型
     * @return          需要设置的值
     */
    public abstract Object onValueSetted(Object value, Field field, DMLType type);
}

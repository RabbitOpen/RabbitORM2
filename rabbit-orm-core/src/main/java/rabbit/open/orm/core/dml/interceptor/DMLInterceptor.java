package rabbit.open.orm.core.dml.interceptor;

import rabbit.open.orm.common.dml.DMLType;

import java.lang.reflect.Field;

/**
 * <b>Description  dml操作前置拦截器</b>
 */
public interface DMLInterceptor {

    /**
     * <b>Description   jdbc存储过程设值前置事件</b>
     * @param value     真实的值
     * @param field     对应的字段
     * @param type      dml操作类型
     * @return          需要设置的值
     */
    public Object onValueSet(Object value, Field field, DMLType type);
    
    /**
     * <b>Description   查询结果设值前置事件</b>
     * @param value     真实的值
     * @param field     对应的字段
     * @return          需要设置的值
     */
    public Object onValueGot(Object value, Field field);
}

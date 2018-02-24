package rabbit.open.orm.dml.filter;

import java.lang.reflect.Field;
import java.util.List;

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
    public Object onValueSetted(Object value, Field field, DMLType type);
    
    /**
     * <b>Description   查询结束后置事件</b>
     * @param result    原始查询结果
     * @param clz       实体类
     * @return          用于替换原始查询结果的数据
     */
    public List<Object> queryCompleted(List<Object> result, Class<?> clz); 
}

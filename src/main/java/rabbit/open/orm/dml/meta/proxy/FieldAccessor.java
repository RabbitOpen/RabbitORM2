package rabbit.open.orm.dml.meta.proxy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Description 字段访问类 </b>
 */
public class FieldAccessor {

    private static Map<Class<?>, List<Field>> cache = new ConcurrentHashMap<>();
    
    /**
     * <b>Description  获取类【clz】中声明的所有字段信息 </b>
     * @param clz
     * @return
     */
    public static List<Field> get(Class<?> clz) {
        if (cache.containsKey(clz)) {
            return cache.get(clz);
        }
        cacheFields(clz);
        return cache.get(clz);
    }

    private synchronized static void cacheFields(Class<?> clz) {
        if (cache.containsKey(clz)) {
            return;
        }
        List<Field> fields = getFields(clz);
        List<Field> proxyFields = new ArrayList<>();
        for (Field f : fields) {
            proxyFields.add(FieldProxy.proxy(f));
        }
        cache.put(clz, proxyFields);
    }

    private static List<Field> getFields(Class<?> clz) {
        Class<?> superClz = clz;
        List<Field> fields = new ArrayList<>();
        //递归获取父类字段
        while (!superClz.equals(Object.class)) {
            fields.addAll(Arrays.asList(superClz.getDeclaredFields()));
            superClz = superClz.getSuperclass();
        }
        return fields;
    }
    
}

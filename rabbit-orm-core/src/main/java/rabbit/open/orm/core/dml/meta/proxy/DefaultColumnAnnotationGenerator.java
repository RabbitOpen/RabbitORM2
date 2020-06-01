package rabbit.open.orm.core.dml.meta.proxy;

import rabbit.open.orm.core.annotation.Column;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认注解生成器
 * @author xiaoqianbin
 * @date 2020/4/20
 **/
public class DefaultColumnAnnotationGenerator implements InvocationHandler {

    // 字段名
    @Column("")
	private String columnName;

    
    private Map<String, Object> map = new HashMap<>();

    /**
     * 代理生成Column注解对象
     * @param fieldName
     * @author xiaoqianbin
     * @date 2020/4/20
     **/
    public static Column proxy(String fieldName) {
        DefaultColumnAnnotationGenerator generator = new DefaultColumnAnnotationGenerator();
        generator.setColumnName(fieldName);
        try {
			Column column = DefaultColumnAnnotationGenerator.class.getDeclaredField("columnName").getAnnotation(Column.class);
			for (Method m : Column.class.getDeclaredMethods()) {
				generator.map.put(m.getName(), m.invoke(column));
	        }
			generator.map.put("value", generator.columnName);
		} catch (Exception e) {
			// TO DO
            // ignore all exception
		}
        return (Column) Proxy.newProxyInstance(DefaultColumnAnnotationGenerator.class.getClassLoader(), new Class<?>[]{Column.class}, generator);
    }

    /**
     * 设置字段名
     * @param	fieldName
     * @author  xiaoqianbin
     * @date    2020/4/20
     **/
    private void setColumnName(String fieldName) {
        this.columnName = camelToUnderline(fieldName).toUpperCase();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (map.containsKey(method.getName())) {
            return map.get(method.getName());
        }
        return null;
    }

    /**
     * 驼峰转下划线
     * @param param
     * @author xiaoqianbin
     * @date 2020/4/20
     **/
    private String camelToUnderline(String param) {
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append("_");
            }
            sb.append(c);
        }
        return sb.toString();
    }

}

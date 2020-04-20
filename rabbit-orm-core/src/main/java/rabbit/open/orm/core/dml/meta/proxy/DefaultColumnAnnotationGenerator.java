package rabbit.open.orm.core.dml.meta.proxy;

import rabbit.open.orm.core.annotation.Column;

import java.lang.annotation.Annotation;
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
public class DefaultColumnAnnotationGenerator implements InvocationHandler, Column {

    // 字段名
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
        for (Method m : Column.class.getDeclaredMethods()) {
            try {
                Method method = DefaultColumnAnnotationGenerator.class.getDeclaredMethod(m.getName());
                generator.map.put(m.getName(), method.invoke(generator));
            } catch (Exception e) {
                // TO DO
                // ignore all exception
            }
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

    @Override
    public String value() {
        return columnName;
    }

    @Override
    public String pattern() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    @Override
    public int length() {
        return 50;
    }

    @Override
    public boolean keyWord() {
        return false;
    }

    @Override
    public boolean dynamic() {
        return false;
    }

    @Override
    public String comment() {
        return "";
    }

    @Override
    public String joinFieldName() {
        return "";
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}

package rabbit.open.orm.core.dml.meta.proxy;

import java.lang.reflect.Method;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import rabbit.open.orm.common.annotation.Column;

/**
 * <b>Description  自定义的注解代理</b>
 */
public class ColumnProxy implements MethodInterceptor {
    
    
    // column 的名字
    private String value;
    
    // column 的格式
    private String pattern;
    
    // column 的长度
    private int length;
    
    // column 是否为keyword
    private boolean keyWord;
    
    // 是否是动态字段
    private boolean dynamic;

    // 字段注释
    private String comment;
    
    public void setRealObject(Column column) {
        this.value = column.value();
        this.pattern = column.pattern();
        this.length = column.length();
        this.keyWord = column.keyWord();
        this.dynamic = column.dynamic();
        this.comment = column.comment();
    }
    
    /**
     * 
     * <b>Description: 代理所有Column注解</b><br>
     * @param column
     * @return
     * 
     */
    public static Column proxy(Column column) {
        if (null == column) {
            return null;
        }
        ColumnProxy proxy = new ColumnProxy();
        proxy.setRealObject(column);
        Enhancer eh = new Enhancer();
        eh.setSuperclass(Column.class);
        eh.setCallback(proxy);
        return (Column) eh.create();
    }
    
    /***
     * 代理session close方法，实现释放连接的功能
     */
    @Override
    public final Object intercept(Object obj, Method method, Object[] args,
            MethodProxy methodproxy) throws Throwable {
        if ("keyWord".equals(method.getName())) {
            return keyWord;
        }
        if ("length".equals(method.getName())) {
            return length;
        }
        if ("pattern".equals(method.getName())) {
            return pattern;
        }
        if ("value".equals(method.getName())) {
            return value;
        }
        if ("dynamic".equals(method.getName())) {
        	return dynamic;
        }
        if ("comment".equals(method.getName())) {
        	return comment;
        }
        return null;
    }
}

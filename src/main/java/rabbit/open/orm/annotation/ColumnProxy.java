package rabbit.open.orm.annotation;

import java.lang.reflect.Method;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

/**
 * <b>Description  注解代理 </b>
 */
public class ColumnProxy implements MethodInterceptor {

    private String value;
    
    private String pattern;
    
    private int length;
    
    private boolean keyWord;
    
    public void setRealObject(Column column) {
        this.value = column.value();
        this.pattern = column.pattern();
        this.length = column.length();
        this.keyWord = column.keyWord();
    }

    /**
     * 
     * <b>Description: 代理所有连接</b><br>
     * @param realSession
     * @return
     * 
     */
    public static Column getProxy(Column column) {
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
        return null;
    }

}

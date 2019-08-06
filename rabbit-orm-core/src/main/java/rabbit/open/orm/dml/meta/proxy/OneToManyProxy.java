package rabbit.open.orm.dml.meta.proxy;

import java.lang.reflect.Method;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import rabbit.open.orm.annotation.OneToMany;

/**
 * <b>Description 自定义的注解代理</b>
 */
public class OneToManyProxy implements MethodInterceptor {

    // 外键名
    private String joinColumn;

    public void setRealObject(OneToMany m2m) {
        this.joinColumn = m2m.joinColumn();
    }

    /**
     * 
     * <b>Description: 代理所有OneToMany注解</b><br>
     * @param o2m
     * @return
     * 
     */
    public static OneToMany proxy(OneToMany o2m) {
        if (null == o2m) {
            return null;
        }
        OneToManyProxy proxy = new OneToManyProxy();
        proxy.setRealObject(o2m);
        Enhancer eh = new Enhancer();
        eh.setSuperclass(OneToMany.class);
        eh.setCallback(proxy);
        return (OneToMany) eh.create();
    }

    /***
     * 代理session close方法，实现释放连接的功能
     */
    @Override
    public final Object intercept(Object obj, Method method, Object[] args,
            MethodProxy methodproxy) throws Throwable {
        if ("joinColumn".equals(method.getName())) {
            return joinColumn;
        }
        return null;
    }
}

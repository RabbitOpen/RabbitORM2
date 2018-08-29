package rabbit.open.orm.dml.meta.proxy;

import java.lang.reflect.Method;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import rabbit.open.orm.annotation.ManyToMany;
import rabbit.open.orm.dml.policy.Policy;

/**
 * <b>Description 自定义的注解代理</b>
 */
public class ManyToManyProxy implements MethodInterceptor {

    // 一端对象在中间表中的外键名
    private String joinColumn;

    // 中间表的名字
    private String joinTable;

    // 多端对象在中间表中的外键名
    private String reverseJoinColumn;

    private Policy policy;

    // 策略为sequence时的sequence的名字
    private String sequence;

    // 中间表的主键字段名
    private String id;

    public void setRealObject(ManyToMany m2m) {
        this.joinColumn = m2m.joinColumn();
        this.joinTable = m2m.joinTable();
        this.reverseJoinColumn = m2m.reverseJoinColumn();
        this.policy = m2m.policy();
        this.sequence = m2m.sequence();
        this.id = m2m.id();
    }

    /**
     * 
     * <b>Description: 代理所有ManyToMany注解</b><br>
     * 
     * @param m2m
     * @return
     * 
     */
    public static ManyToMany proxy(ManyToMany m2m) {
        if (null == m2m) {
            return null;
        }
        ManyToManyProxy proxy = new ManyToManyProxy();
        proxy.setRealObject(m2m);
        Enhancer eh = new Enhancer();
        eh.setSuperclass(ManyToMany.class);
        eh.setCallback(proxy);
        return (ManyToMany) eh.create();
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
        if ("joinTable".equals(method.getName())) {
            return joinTable;
        }
        if ("reverseJoinColumn".equals(method.getName())) {
            return reverseJoinColumn;
        }
        if ("policy".equals(method.getName())) {
            return policy;
        }
        if ("sequence".equals(method.getName())) {
            return sequence;
        }
        if ("id".equals(method.getName())) {
            return id;
        }
        return null;
    }
}

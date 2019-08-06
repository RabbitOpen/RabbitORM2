package rabbit.open.orm.core.dml.meta.proxy;

import java.lang.reflect.Method;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import rabbit.open.orm.common.annotation.PrimaryKey;
import rabbit.open.orm.common.dml.Policy;

/**
 * <b>Description  自定义的注解代理</b>
 */
public class PrimaryKeyProxy implements MethodInterceptor {
    
    
    // 主键策略的名字
    private Policy policy;
    
    // 主键序列的名字
    private String sequence;
    
    public void setRealObject(PrimaryKey primaryKey) {
        this.policy = primaryKey.policy();
        this.sequence = primaryKey.sequence();
    }
    
    /**
     * 
     * <b>Description: 代理所有PrimaryKey注解</b><br>
     * @param primaryKey
     * @return
     * 
     */
    public static PrimaryKey proxy(PrimaryKey primaryKey) {
        if (null == primaryKey) {
            return null;
        }
        PrimaryKeyProxy proxy = new PrimaryKeyProxy();
        proxy.setRealObject(primaryKey);
        Enhancer eh = new Enhancer();
        eh.setSuperclass(PrimaryKey.class);
        eh.setCallback(proxy);
        return (PrimaryKey) eh.create();
    }
    
    /***
     * 代理session close方法，实现释放连接的功能
     */
    @Override
    public final Object intercept(Object obj, Method method, Object[] args,
            MethodProxy methodproxy) throws Throwable {
        if ("policy".equals(method.getName())) {
            return policy;
        }
        if ("sequence".equals(method.getName())) {
            return sequence;
        }
        return null;
    }
}

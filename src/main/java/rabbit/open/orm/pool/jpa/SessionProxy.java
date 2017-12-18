package rabbit.open.orm.pool.jpa;

import java.lang.reflect.Method;
import java.sql.Connection;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import rabbit.open.orm.pool.SessionFactory;

/**
 * <b>Description:   代理session，代理close方法，调用SessionFactory的releaseConnection方法</b>.
 * <b>@author</b>    肖乾斌
 * 
 */
public class SessionProxy implements MethodInterceptor{

    //真实session
    private Connection realSession;
    
    public void setRealSession(Connection realSession) {
        this.realSession = realSession;
    }
    
    /**
     * 
     * <b>Description:  代理所有连接</b><br>.
     * @param realSession
     * @return	
     * 
     */
    public static Connection getProxy(Connection realSession){
        SessionProxy proxy = new SessionProxy();
        proxy.setRealSession(realSession);
        Enhancer eh = new Enhancer();
        eh.setSuperclass(Connection.class);
        eh.setCallback(proxy);
        return (Connection) eh.create();
    }

    /***
     * 代理session close方法，实现释放连接的功能
     */
    @Override
    public final Object intercept(Object obj, Method method, Object[] args,
            MethodProxy methodproxy) throws Throwable {
        if("close".equals(method.getName())){
            SessionFactory.releaseConnection(realSession);
            return null;
        }
        return method.invoke(realSession, args);
    }

}

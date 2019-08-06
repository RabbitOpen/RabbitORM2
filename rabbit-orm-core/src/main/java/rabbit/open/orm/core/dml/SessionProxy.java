package rabbit.open.orm.core.dml;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

/**
 * <b>Description: 代理session，代理close方法，调用SessionFactory的releaseConnection方法</b>.
 * <b>@author</b> 肖乾斌
 * 
 */
public class SessionProxy implements MethodInterceptor {

	private static Logger logger = Logger.getLogger(SessionProxy.class);
	
    // 真实session
    private Connection realSession;
    
    private int transactionIsolation = -1;

    public void setRealSession(Connection realSession) {
        this.realSession = realSession;
    }

    /**
     * <b>@description 获取真实连接的事务隔离级别 </b>
     * @return
     */
    public int getTransactionIsolation() {
    	if (-1 == transactionIsolation) {
    		try {
				transactionIsolation = realSession.getTransactionIsolation();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
    	}
		return transactionIsolation;
	}
	
	/**
	 * <b>@description 设置真实连接的事务隔离级别 </b>
	 * @param level
	 * @throws SQLException 
	 */
	public void setTransactionIsolation(int level) throws SQLException {
		if (getTransactionIsolation() != level) {
			this.transactionIsolation = level;
			realSession.setTransactionIsolation(level);
		}
	}
	
    /**
     * 
     * <b>Description: 代理所有连接</b><br>
     * @param realSession
     * @return
     * 
     */
    public static Connection getProxy(Connection realSession) {
        SessionProxy proxy = new SessionProxy();
        proxy.setRealSession(realSession);
        Enhancer eh = new Enhancer();
        eh.setSuperclass(Connection.class);
        eh.setCallback(proxy);
        return (Connection) eh.create();
    }

    /***
     * 代理部分方法
     */
    @Override
    public final Object intercept(Object obj, Method method, Object[] args,
            MethodProxy methodproxy) throws Throwable {
        if ("close".equals(method.getName())) {
        	// 代理close方法，实现释放连接的功能
            SessionFactory.releaseConnection(realSession);
            return null;
        }
        // 访问事务隔离级别时调用代理方法
        if ("setTransactionIsolation".equals(method.getName())) {
        	setTransactionIsolation((int) args[0]);
        	return null;
        }
        if ("getTransactionIsolation".equals(method.getName())) {
        	return getTransactionIsolation();
        }
        return method.invoke(realSession, args);
    }

}

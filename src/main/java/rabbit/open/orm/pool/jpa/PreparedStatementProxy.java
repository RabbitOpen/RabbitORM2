package rabbit.open.orm.pool.jpa;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import rabbit.open.orm.exception.RabbitDMLException;

public class PreparedStatementProxy implements MethodInterceptor {

    //真实的jdbc存储过程
    private PreparedStatement stmt;
    
    static Class<?> oraclePreparedStatementClz = null;
    
    static {
        try {
            oraclePreparedStatementClz = Class.forName("oracle.jdbc.OraclePreparedStatement");
        } catch (ClassNotFoundException e) {
            
        }
    }
    
    public void setStmt(PreparedStatement stmt) {
        this.stmt = stmt;
    }
    
    public static PreparedStatement getProxy(PreparedStatement stmt){
        PreparedStatementProxy proxy = new PreparedStatementProxy();
        proxy.setStmt(stmt);
        Enhancer eh = new Enhancer();
        if(null != oraclePreparedStatementClz && oraclePreparedStatementClz.isAssignableFrom(stmt.getClass())){
            eh.setSuperclass(oraclePreparedStatementClz);
        }else{
            eh.setSuperclass(PreparedStatement.class);
        }
        eh.setCallback(proxy);
        return (PreparedStatement) eh.create();
    }

    /**
     * 
     * <b>Description:  销毁jdbc存储过程  </b><br>.	
     * 
     */
    public void destroy(){
        try {
            this.stmt.close();
        } catch (SQLException e) {
            throw new RabbitDMLException(e);
        }
    }
    
    /***
     * 代理PreparedStatement close方法，什么都不做
     */
    @Override
    public final Object intercept(Object obj, Method method, Object[] args,
            MethodProxy methodproxy) throws Throwable {
        if("close".equals(method.getName())){
            return null;
        }
        return method.invoke(stmt, args);
    }
}

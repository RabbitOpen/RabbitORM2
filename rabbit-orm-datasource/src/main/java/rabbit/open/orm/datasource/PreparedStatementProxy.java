package rabbit.open.orm.datasource;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import rabbit.open.orm.common.exception.RabbitDMLException;

public class PreparedStatementProxy implements MethodInterceptor {

    //真实的jdbc存储过程
    private PreparedStatement stmt;
    
    static Class<?> oraclePreparedStatementClz = null;
    
    private StringBuilder preparedSql;
    
    private RabbitDataSource dataSource;
    
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    static {
        try {
            oraclePreparedStatementClz = Class.forName("oracle.jdbc.OraclePreparedStatement");
        } catch (ClassNotFoundException e) {
            // TO DO: ignore
        }
    }
    
    public void setStmt(PreparedStatement stmt) {
        this.stmt = stmt;
    }
    
	public static PreparedStatement getProxy(PreparedStatement stmt,
			String sql, RabbitDataSource dataSource) {
		PreparedStatementProxy proxy = new PreparedStatementProxy();
		proxy.preparedSql = new StringBuilder(sql);
		proxy.dataSource = dataSource;
		proxy.setStmt(stmt);
		Enhancer eh = new Enhancer();
		if (null != oraclePreparedStatementClz && oraclePreparedStatementClz.isAssignableFrom(stmt.getClass())) {
			eh.setSuperclass(oraclePreparedStatementClz);
		} else {
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
	public void destroy() {
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
		if ("close".equals(method.getName())) {
			return null;
		}
		if (showSlowSql() && method.getName().startsWith("execute")) {
			long start = System.currentTimeMillis();
			Object ret = method.invoke(stmt, args);
			long cost = System.currentTimeMillis() - start;
			if (cost >= getSlowSqlThreshold()) {
				logger.debug("cost: {}ms, {}", cost, preparedSql);
			}
			return ret;
		}
        return method.invoke(stmt, args);
    }

	/**
	 * <b>Description 获取慢sql阈值 </b>
	 * @return
	 * @author 肖乾斌
	 */
	private long getSlowSqlThreshold() {
		return dataSource.getThreshold();
	}

	/**
	 * <b>Description 判断是否显示慢sql </b>
	 * @return
	 * @author 肖乾斌
	 */
	private boolean showSlowSql() {
		return dataSource.isShowSlowSql();
	}

}

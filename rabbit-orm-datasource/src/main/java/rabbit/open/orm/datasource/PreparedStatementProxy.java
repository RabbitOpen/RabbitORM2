package rabbit.open.orm.datasource;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import rabbit.open.orm.common.exception.RabbitDMLException;

public class PreparedStatementProxy implements MethodInterceptor {

    //真实的jdbc存储过程
    private PreparedStatement stmt;
    
    static Class<?> oraclePreparedStatementClz = null;
    
    private List<Object> parameters = new ArrayList<>();
    
    private StringBuilder preparedSql;
    
    private RabbitDataSource dataSource;
    
    private Logger logger = Logger.getLogger(getClass());
    
    static {
        try {
            oraclePreparedStatementClz = Class.forName("oracle.jdbc.OraclePreparedStatement");
        } catch (ClassNotFoundException e) {
            
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
		if (showSlowSql()) {
			if (method.getName().startsWith("set")) {
				parameters.add((int)args[0] - 1, args[1]);
			}
			if (method.getName().startsWith("execute")) {
				long start = System.currentTimeMillis();
				Object ret = method.invoke(stmt, args);
				long cost = System.currentTimeMillis() - start;
				if (cost >= getSlowSqlThreshold()) {
					logger.debug("cost: " + cost + "ms, " + getSqlText());
				}
				parameters.clear();
				return ret;
			}
		}
        return method.invoke(stmt, args);
    }

	/**
	 * <b>Description 获取慢sql文本  </b>
	 * @return
	 * @author 肖乾斌
	 */
	private String getSqlText() {
		String sql = preparedSql.toString(); 
		for (int i = 0; i < parameters.size(); i++) {
			sql = sql.replaceFirst("\\?", getString(parameters.get(i)));
		}
		return sql;
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
    
    private String getString(Object o) {
    	if (null == o) {
    		return "null";
    	}
    	if (o instanceof Date) {
    		return "'" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(o) + "'";
    	}
    	return "'" + o.toString() + "'";
    }
}

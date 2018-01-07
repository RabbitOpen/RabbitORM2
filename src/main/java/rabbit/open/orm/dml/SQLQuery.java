package rabbit.open.orm.dml;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import rabbit.open.orm.dml.name.SQLObject;
import rabbit.open.orm.dml.name.SQLParser;
import rabbit.open.orm.dml.util.SQLFormater;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.pool.SessionFactory;

public class SQLQuery<T> {

    Logger logger = Logger.getLogger(getClass());
    
	private SessionFactory sessionFactory;
	
	private SQLObject query;
	
	private SQLCallBack<T> callBack;
	
	public SQLQuery(SessionFactory sessionFactory, String queryName, Class<?> clz, SQLCallBack<T> callBack) {
		super();
		this.sessionFactory = sessionFactory;
		query = SQLParser.getNamedJdbcQuery(queryName, clz);
		this.callBack = callBack;
	}

	/**
	 * 
	 * <b>Description:	执行原生sql语句</b><br>
	 * @return
	 * 
	 */
	public T execute() {
		Connection conn = null;
		PreparedStatement stmt = null;
		try{
			conn = sessionFactory.getConnection();
			showSQL(query.getSql());
			stmt = conn.prepareStatement(query.getSql());
            return callBack.execute(stmt);
		} catch (Exception e){
			throw new RabbitDMLException(e.getMessage(), e);
		} finally {
		    DMLAdapter.closeConnection(conn);
		    DMLAdapter.closeStmt(stmt);
		}
	}
	
	private void showSQL(String sql){
	    logger.info("\n" + (sessionFactory.isFormatSql() ? 
                SQLFormater.format(sql) : sql));
	}

}

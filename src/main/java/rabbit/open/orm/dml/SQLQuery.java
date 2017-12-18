package rabbit.open.orm.dml;

import java.sql.Connection;
import java.sql.PreparedStatement;

import rabbit.open.orm.dml.xml.NameQuery;
import rabbit.open.orm.dml.xml.SQLParser;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.pool.SessionFactory;

public class SQLQuery<T> {

	private SessionFactory sessionFactory;
	
	private NameQuery query;
	
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
			stmt = conn.prepareStatement(query.getSql());
            return callBack.execute(stmt);
		} catch (Exception e){
			throw new RabbitDMLException(e.getMessage(), e);
		} finally {
		    DMLAdapter.closeConnection(conn);
		    DMLAdapter.closeStmt(stmt);
		}
	}

}

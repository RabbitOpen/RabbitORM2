package rabbit.open.orm.core.dml;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dml.name.SQLObject;
import rabbit.open.orm.core.utils.SQLFormater;
import rabbit.open.orm.datasource.Session;

public class SQLQuery<T> {

    Logger logger = Logger.getLogger(getClass());
    
	private SessionFactory sessionFactory;
	
	private SQLObject query;
	
	private Class<?> clz;
	
	private SQLCallBack<T> callBack;
	
	public SQLQuery(SessionFactory sessionFactory, String queryName, Class<?> clz, SQLCallBack<T> callBack) {
		super();
		this.sessionFactory = sessionFactory;
		query = sessionFactory.getNamedJdbcQuery(queryName, clz);
		this.clz = clz;
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
		try {
			conn = sessionFactory.getConnection(clz, null, DMLType.SELECT);
			showSQL(query.getSql());
			stmt = conn.prepareStatement(query.getSql());
            return callBack.execute(stmt);
		} catch (Exception e) {
		    Session.flagException();
			throw new RabbitDMLException(e.getMessage(), e);
		} finally {
		    DMLAdapter.closeStmt(stmt);
		    DMLAdapter.closeConnection(conn);
		    Session.clearException();
		}
	}
	
	public void showSQL(String sql) {
		if (sessionFactory.isShowSql()) {
			logger.info("\n"
					+ (sessionFactory.isFormatSql() ? SQLFormater.format(sql)
							: sql));
		}
	}

}

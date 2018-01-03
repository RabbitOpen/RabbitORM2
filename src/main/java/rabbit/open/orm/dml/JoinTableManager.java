package rabbit.open.orm.dml;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import rabbit.open.orm.annotation.ManyToMany;
import rabbit.open.orm.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.dml.meta.MetaData;
import rabbit.open.orm.dml.meta.PreparedSqlDescriptor;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.pool.SessionFactory;

/**
 * <b>Description: 	中间表管理器</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public class JoinTableManager<T> extends NonQueryAdapter<T>{

	private static final String EMPTY_PRIMARY_KEY = "the value of primary key can't be null!";

    public JoinTableManager(SessionFactory sessionFactory, Class<T> clz) {
		super(sessionFactory, clz);
	}

	/**
	 * 
	 * <b>Description:	向many2many的中间表中插入数据</b><br>
	 * @param data
	 * @return
	 * 
	 */
	public long addJoinRecords(T data){
		Field pk = MetaData.getPrimaryKeyField(metaData.getEntityClz());
		pk.setAccessible(true);
		Object value;
		try {
			value = getFieldValue(data, pk);
		} catch (Exception e) {
			throw new RabbitDMLException(e.getMessage(), e);
		}
		if(null == value){
			throw new RabbitDMLException(EMPTY_PRIMARY_KEY);
		}
		this.sqlOperation = new SQLOperation(){

			@Override
			public long executeSQL(Connection conn) throws Exception {
				List<PreparedSqlDescriptor> psds = createAddJoinRecordsSql(data, value);
				executeBatch(conn, psds, 0);
				return 0;
			}
		};
		return execute();
	}
	
	/**
	 * 
	 * <b>Description:	从many2many的中间表中移除数据</b><br>
	 * @param data
	 * @return
	 * 
	 */
	public long removeJoinRecords(T data){
		Field pk = MetaData.getPrimaryKeyField(metaData.getEntityClz());
		pk.setAccessible(true);
		Object value = getFieldValue(data, pk);
		if(null == value){
			throw new RabbitDMLException(EMPTY_PRIMARY_KEY);
		}
		this.sqlOperation = new SQLOperation(){
			@Override
			public long executeSQL(Connection conn) throws Exception {
				List<PreparedSqlDescriptor> psds = createRemoveJoinRecordsSql(data, value);
				if(psds.isEmpty()){
					throw new RabbitDMLException("no record to remove!");
				}
				executeBatch(conn, psds, 0);
				return 0;
			}
		};
		return execute();
	}

	private Object getFieldValue(T data, Field pk){
		try {
			return pk.get(data);
		} catch (IllegalAccessException e) {
			throw new RabbitDMLException(e.getMessage(), e);
		}
	}
	
	/**
	 * 
	 * <b>Description:	清除多对多记录</b><br>
	 * @param 	data
	 * @param 	join
	 * 
	 */
	public void clearJoinRecords(T data, Class<?> join) {
		Field pk = MetaData.getPrimaryKeyField(metaData.getEntityClz());
		pk.setAccessible(true);
		Object value;
		value = getFieldValue(data, pk);
		if(null == value){
			throw new RabbitDMLException(EMPTY_PRIMARY_KEY);
		}
		sql = new StringBuilder();
		for(JoinFieldMetaData<?> jfm : metaData.getJoinMetas()){
			if(!(jfm.getAnnotation() instanceof ManyToMany) || !jfm.getJoinClass().equals(join)){
				continue;
			}
			ManyToMany mtm = (ManyToMany) jfm.getAnnotation();
			sql.append("DELETE FROM " + mtm.joinTable() + " WHERE ");
			sql.append(mtm.joinColumn() + " = ");
			sql.append(PLACE_HOLDER);
			preparedValues.add(RabbitValueConverter.convert(value, getPrimayKeyFieldMeta(metaData.getEntityClz())));
		}
		if(0 == sql.length()){
			throw new RabbitDMLException("no record to clear!");
		}
		this.sqlOperation = new SQLOperation(){
			@Override
			public long executeSQL(Connection conn) throws Exception {
			    PreparedStatement stmt = null;
                try {
                    showSql();
                    stmt = conn.prepareStatement(sql.toString());
                    setPreparedStatementValue(stmt);
                    return stmt.executeUpdate();
                } finally {
                    closeStmt(stmt);
                }
			}
		};
		execute();
	}
	
	/**
	 * 
	 * <b>Description:	从many2many的中间表中替换数据(先移除相同的再添加)</b><br>
	 * @param data
	 * 
	 */
	public void replaceJoinRecords(T data) {
		Field pk = MetaData.getPrimaryKeyField(metaData.getEntityClz());
		pk.setAccessible(true);
		Object value = getFieldValue(data, pk);
		if(null == value){
			throw new RabbitDMLException(EMPTY_PRIMARY_KEY);
		}
		this.sqlOperation = new SQLOperation(){
			@Override
			public long executeSQL(Connection conn) throws Exception {
				List<PreparedSqlDescriptor> psds2r = createRemoveJoinRecordsSql(data, value);
				int counter = executeBatch(conn, psds2r, 0);
				List<PreparedSqlDescriptor> psds = createAddJoinRecordsSql(data, value);
				executeBatch(conn, psds, counter);
				return 0;
			}
		};
		execute();
	}
}

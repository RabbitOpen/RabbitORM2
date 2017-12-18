package rabbit.open.orm.dml;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;

import rabbit.open.orm.annotation.Relation.FilterType;
import rabbit.open.orm.dialect.dml.DeleteDialectAdapter;
import rabbit.open.orm.dml.meta.FieldMetaData;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.pool.SessionFactory;

/**
 * <b>Description: 	删除操作</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public class Delete<T> extends NonQueryAdapter<T> {

	public Delete(SessionFactory sessionFactory, Class<T> clz) {
		this(sessionFactory, null, clz);
	}

	public Delete(SessionFactory sessionFactory, T filterData, Class<T> clz) {
		super(sessionFactory, filterData, clz);
		sqlOperation = new SQLOperation() {
			@Override
			public long executeSQL(Connection conn) throws Exception {
			    PreparedStatement stmt = null;
				try{
				    prepareFilterMetas();
	                createDeleteSql();
	                showSql();
	                stmt = conn.prepareStatement(sql.toString());
	                setPreparedStatementValue(stmt);
	                return stmt.executeUpdate();
				} finally {
				    closeStmt(stmt);
				}
			}
		};
	}

	/**
	 * 
	 * <b>Description:	生成删除sql</b><br>
	 * 
	 */
	private void createDeleteSql() {
		combineFilters();
		if(filterDescriptors.isEmpty()){
			sql = new StringBuilder("DELETE FROM " + metaData.getTableName());
			return;
		}
		DeleteDialectAdapter generator = DeleteDialectAdapter.getDialectGenerator(sessionFactory.getDialectType());
		sql = generator.createDeleteSql(this);
	}
	
	/**
	 * 
	 * <b>Description:	根据主键删除数据</b><br>
	 * @param 	id
	 * @return
	 * 
	 */
	public long deleteById(Serializable id){
		if(null == id){
			throw new RabbitDMLException("id can't be null");
		}
		sql = new StringBuilder("DELETE FROM " + metaData.getTableName() + " WHERE ");
		for(FieldMetaData fmd : metaData.getFieldMetas()){
			if(!fmd.isPrimaryKey()){
				continue;
			}
			preparedValues.add(RabbitValueConverter.convert(id, fmd));
			sql.append(fmd.getColumn().value() + " = " + PLACE_HOLDER);
		}
		sqlOperation = new SQLOperation(){
			@Override
			public long executeSQL(Connection conn) throws Exception {
			    PreparedStatement stmt = null;
				try{
				    showSql();
	                stmt = conn.prepareStatement(sql.toString());
	                setPreparedStatementValue(stmt);
	                return stmt.executeUpdate();
				} finally {
				    closeStmt(stmt);
				}
			}
		};
		return execute();
	}
	
	/**
	 * 
	 * <b>Description:	动态新增一个过滤条件</b><br>
	 * @param fieldReg
	 * @param value
	 * @param ft
	 * @param depsPath
	 * @return
	 * 
	 */
	@Override
	public Delete<T> addFilter(String fieldReg, Object value, FilterType ft, 
			Class<?>... depsPath){
		super.addFilter(fieldReg, value, ft, depsPath);
		return this;
	}

	/**
	 * 
	 * <b>Description:	动态新增一个过滤条件</b><br>
	 * @param fieldReg
	 * @param value
	 * @param depsPath
	 * @return
	 * 
	 */
	@Override
	public Delete<T> addFilter(String fieldReg, Object value,
			Class<?>... depsPath) {
		super.addFilter(fieldReg, value, FilterType.EQUAL, depsPath);
		return this;
	}
	
	/**
	 * 
	 * <b>Description:	新增一个空过滤条件</b><br>
	 * @param fieldReg
	 * @param isNull	true -> is, false -> is not
	 * @param depsPath
	 * @return	
	 * 
	 */
	@Override
	public Delete<T> addNullFilter(String fieldReg, boolean isNull, Class<?>... depsPath) {
		super.addFilter(fieldReg, null, isNull ? FilterType.IS : FilterType.IS_NOT, depsPath);
		return this;
	}

	/**
	 * 
	 * <b>Description:	新增一个空过滤条件</b><br>
	 * @param fieldReg
	 * @param depsPath
	 * @return	
	 * 
	 */
	@Override
	public Delete<T> addNullFilter(String fieldReg, Class<?>... depsPath) {
		super.addNullFilter(fieldReg, depsPath);
		return this;
	}
}

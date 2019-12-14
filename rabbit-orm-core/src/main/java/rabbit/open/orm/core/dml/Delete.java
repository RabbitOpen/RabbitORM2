package rabbit.open.orm.core.dml;

import java.io.Serializable;
import java.sql.PreparedStatement;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dialect.dml.DeleteDialectAdapter;
import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.MultiDropFilter;
import rabbit.open.orm.core.dml.shard.ShardFactor;

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
		setDmlType(DMLType.DELETE);
		sqlOperation = conn -> {
			PreparedStatement stmt = null;
			try {
				createDeleteSql();
				stmt = conn.prepareStatement(sql.toString());
				setPreparedStatementValue(stmt, DMLType.DELETE);
				showSql();
				return stmt.executeUpdate();
			} finally {
				closeStmt(stmt);
			}
		};
	}
	
	@Override
	public long execute() {
		resetForExecute();
		runCallBackTask();
	    prepareFilterMetas();
        mergeFilters();
        doShardingCheck();
	    return super.execute();
	}
	
	/**
	 * 
	 * <b>Description:	生成删除sql</b><br>
	 * 
	 */
	private void createDeleteSql() {
		if (filterDescriptors.isEmpty()) {
			sql = new StringBuilder("DELETE FROM " + metaData.getTableName());
			StringBuilder mds = createMultiDropSql();
			if (0 != mds.length()) {
				mds.insert(0, WHERE + " 1 = 1 ");
				sql.append(mds);
			}
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
	public long deleteById(Serializable id) {
		if (null == id) {
			throw new RabbitDMLException("id can't be null");
		}
		sql = new StringBuilder("DELETE FROM " + TARGET_TABLE_NAME + WHERE);
		for (FieldMetaData fmd : metaData.getFieldMetas()) {
			if (!fmd.isPrimaryKey()) {
				continue;
			}
			factors.add(new ShardFactor(fmd.getField(), FilterType.EQUAL.value(), id));
			preparedValues.add(new PreparedValue(RabbitValueConverter.convertByField(id, fmd), fmd.getField()));
			sql.append(getColumnName(fmd.getColumn()) + " = " + PLACE_HOLDER);
		}
		sqlOperation = conn -> {
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(sql.toString());
				setPreparedStatementValue(stmt, DMLType.DELETE);
				showSql();
				return stmt.executeUpdate();
			} finally {
				closeStmt(stmt);
			}
		};
		updateTargetTableName();
		return super.execute();
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
			Class<?>... depsPath) {
		super.addFilter(fieldReg, value, ft, depsPath);
		return this;
	}

	/**
     * <b>Description  添加Or类型的过滤条件</b>
     * @param multiDropFilter
     */
    public Delete<T> addMultiDropFilter(MultiDropFilter multiDropFilter) {
        cacheMultiDropFilter(multiDropFilter);
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
		super.addNullFilter(fieldReg, isNull, depsPath);
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
	
	/**
	 * 
	 * <b>Description:	新增一个非空过滤条件</b><br>
	 * @param fieldReg
	 * @param depsPath
	 * @return	
	 * 
	 */
	@Override
	public Delete<T> addNotNullFilter(String fieldReg, Class<?>... depsPath) {
		super.addNotNullFilter(fieldReg, depsPath);
		return this;
	}
	
}

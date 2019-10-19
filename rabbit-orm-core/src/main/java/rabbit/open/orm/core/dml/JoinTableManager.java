package rabbit.open.orm.core.dml;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rabbit.open.orm.common.annotation.ManyToMany;
import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.exception.InvalidJoinMergeException;
import rabbit.open.orm.common.shard.ShardFactor;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.PreparedSqlDescriptor;

/**
 * <b>Description: 中间表管理器</b><br>
 * <b>@author</b> 肖乾斌
 * @param <T>
 * 
 */
@SuppressWarnings("rawtypes")
public class JoinTableManager<T> extends NonQueryAdapter<T> {

    public JoinTableManager(SessionFactory sessionFactory, Class<T> clz) {
        super(sessionFactory, clz);
        setDmlType(DMLType.UPDATE);
    }

    /**
     * 
     * <b>Description: 向many2many的中间表中插入数据</b><br>
     * @param data
     * @return
     * 
     */
    public void addJoinRecords(T data) {
    	for (JoinFieldMetaData<?> jfm : metaData.getJoinMetas()) {
    		addJoinRecords(data, jfm.getJoinClass());
    	}
    }
    
    /**
     * 
     * <b>@description 添加与指定对象的中间表数据 </b>
     * @param data		
     * @param joinClass	指定对象
     * @return
     */
	public void addJoinRecords(T data, Class joinClass) {
		this.sqlOperation = conn -> {
			preparedValues.clear();
	        for (JoinFieldMetaData<?> jfm : metaData.getJoinMetas()) {
	        	if (joinClass.equals(jfm.getJoinClass()) && (jfm.getAnnotation() instanceof ManyToMany)) {
	        		PreparedSqlDescriptor psd = createAddJoinRecordsSql(data, jfm);
	        		if (null != psd) {
	        			executeBatch(conn, Arrays.asList(psd));
	        		}
	        		return 0;
	        	}
	        }
			return 0;
		};
        setDmlType(DMLType.INSERT);
        execute();
    }

    /**
     * 用主表名替代中间表名
     */
    @Override
    protected String getCurrentTableName() {
        return getDeclaredTableName();
    }

    @Override
    protected List<ShardFactor> getFactors() {
        return new ArrayList<>();
    }

    /**
     * 
     * <b>Description: 从many2many的中间表中移除数据</b><br>
     * @param data
     * @return
     * 
     */
    public void removeJoinRecords(T data) {
    	for (JoinFieldMetaData<?> jfm : metaData.getJoinMetas()) {
    		removeJoinRecords(data, jfm.getJoinClass());
    	}
    }

    /**
     * 
     * <b>Description: 清除多对多记录</b><br>
     * @param data
     * @param joinClass
     * 
     */
    public void removeJoinRecords(T data, Class<?> joinClass) {
    	preparedValues.clear();
        sql = new StringBuilder();
        for (JoinFieldMetaData<?> jfm : metaData.getJoinMetas()) {
            if (!(jfm.getAnnotation() instanceof ManyToMany)
                    || !jfm.getJoinClass().equals(joinClass)) {
                continue;
            }
            ManyToMany mtm = (ManyToMany) jfm.getAnnotation();
            sql.append("DELETE FROM " + mtm.joinTable() + WHERE);
            sql.append(mtm.joinColumn() + " = ");
            sql.append(PLACE_HOLDER);
            FieldMetaData fmd = MetaData.getCachedFieldsMeta(getEntityClz(), jfm.getMasterField().getName());
            Object value = getValue(jfm.getMasterField(), data);
            assertEmptyPKValue(value);
			preparedValues.add(new PreparedValue(RabbitValueConverter.convert(
            		value, fmd), fmd.getField()));
        }
        if (0 == sql.length()) {
            return;
        }
        this.sqlOperation = conn -> {
            PreparedStatement stmt = null;
            try {
                stmt = conn.prepareStatement(sql.toString());
                setPreparedStatementValue(stmt, null);
                showSql();
                return stmt.executeUpdate();
            } finally {
                closeStmt(stmt);
            }
        };
        setDmlType(DMLType.DELETE);
        execute();
    }

    /**
     * 
     * <b>Description: 从many2many的中间表中替换数据(先移除所有旧的再添加新的)</b><br>
     * @param data
     * @param joinClass
     */
    public void replaceJoinRecords(T data, Class joinClass) {
    	removeJoinRecords(data, joinClass);
    	addJoinRecords(data, joinClass);
    }
    
    /**
     * 
     * <b>@description 用data中的中间表数据和数据库中的进行合并 </b>
     * @param data
     * @param joinClass
     */
    public void mergeJoinRecords(T data, Class joinClass) {
    	removeJoinRecordsByDataAndJoinClass(data, joinClass);
        addJoinRecords(data, joinClass);
    }

	private void removeJoinRecordsByDataAndJoinClass(T data, Class joinClass) {
		this.sqlOperation = conn -> {
			preparedValues.clear();
			JoinFieldMetaData jfmd = getJoinFieldMetaData(joinClass);
	    	if (null == jfmd) {
	    		throw new InvalidJoinMergeException(getEntityClz(), joinClass);
	    	}
	    	List<PreparedSqlDescriptor> psd = createRemoveJoinRecordsSql(data, jfmd);
	    	executeBatch(conn, psd);
			return 0;
		};
        setDmlType(DMLType.DELETE);
        execute();
	}

	private JoinFieldMetaData getJoinFieldMetaData(Class joinClass) {
		for (JoinFieldMetaData<?> jfm : metaData.getJoinMetas()) {
            if ((jfm.getAnnotation() instanceof ManyToMany)
                    && jfm.getJoinClass().equals(joinClass)) {
            	return jfm;
            }
    	}
		return null;
	}
    
    /**
	 * 
	 * <b>Description:	创建删除中间表记录的sql</b><br>
	 * @param data
	 * @param jfm
	 * @return
	 * 
	 */
	protected List<PreparedSqlDescriptor> createRemoveJoinRecordsSql(T data, JoinFieldMetaData<?> jfm) {
		List<PreparedSqlDescriptor> descriptors = new ArrayList<>();
		List<?> joinRecords = getM2MJoinFieldValue(data, jfm);
		if (joinRecords.isEmpty()) {
			return descriptors;
		}
		PreparedSqlDescriptor psd = new PreparedSqlDescriptor(1);
		ManyToMany mtm = (ManyToMany) jfm.getAnnotation();
		StringBuilder rsql = new StringBuilder();
		rsql.append("DELETE FROM " + mtm.joinTable() + WHERE);
		FieldMetaData pkfmd = MetaData.getCachedFieldsMeta(getEntityClz(), jfm.getMasterField().getName());
		Object value = getValue(jfm.getMasterField(), data);
		assertEmptyPKValue(value);
		Object pv = RabbitValueConverter.convert(value, pkfmd);
		List<Object> values = new ArrayList<>();
		values.add(new PreparedValue(pv, pkfmd.getField()));
		rsql.append(mtm.joinColumn() + " = " + PLACE_HOLDER);
		rsql.append(" AND " + mtm.reverseJoinColumn() + " IN (");
		for (Object o : joinRecords) {
			// 子表的关联主键值
			Object jpkv = getValue(jfm.getSlaveField(), o);
			FieldMetaData fmd = MetaData.getCachedFieldsMeta(jfm.getJoinClass(), jfm.getSlaveField().getName());
			values.add(new PreparedValue(RabbitValueConverter.convert(jpkv, fmd), fmd.getField()));
			rsql.append("?, ");
		}
		rsql.deleteCharAt(rsql.lastIndexOf(","));
		rsql.append(")");
		preparedValues.add(values);
		psd.setSql(rsql);
		descriptors.add(psd);
		return descriptors;
	}
    
}

package rabbit.open.orm.dml;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rabbit.open.orm.annotation.FilterType;
import rabbit.open.orm.annotation.ManyToMany;
import rabbit.open.orm.dml.filter.PreparedValue;
import rabbit.open.orm.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.dml.meta.FieldMetaData;
import rabbit.open.orm.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.dml.meta.MetaData;
import rabbit.open.orm.dml.meta.PreparedSqlDescriptor;
import rabbit.open.orm.dml.policy.Policy;
import rabbit.open.orm.dml.policy.UUIDPolicy;
import rabbit.open.orm.dml.util.SQLFormater;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.exception.UnKnownFieldException;
import rabbit.open.orm.pool.SessionFactory;

/**
 * <b>Description: 	非查询操作的适配器</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public abstract class NonQueryAdapter<T> extends DMLAdapter<T>{

	protected SQLOperation sqlOperation;
	
	public NonQueryAdapter(SessionFactory sessionFactory, Class<T> clz) {
		super(sessionFactory, clz);
	}

	public NonQueryAdapter(SessionFactory sessionFactory, T filterData,
			Class<T> clz) {
		super(sessionFactory, filterData, clz);
	}

	protected interface SQLOperation{
		
		/**
		 * 
		 * <b>Description:	Description:	执行sql操作</b><br>
		 * @param conn
		 * @return
		 * @throws Exception	
		 * 
		 */
		public long executeSQL(Connection conn) throws Exception;
	}
	
	/**
	 * 
	 * <b>Description:	执行sql</b><br>
	 * @return
	 * 
	 */
	public long execute(){
		Connection conn = null;
		try{
			conn = sessionFactory.getConnection();
			return sqlOperation.executeSQL(conn);
		} catch (UnKnownFieldException e){
			throw e;
		} catch (Exception e){
		    throw new RabbitDMLException(e.getMessage(), e);
		} finally {
			closeConnection(conn);
		}
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
	public NonQueryAdapter<T> addFilter(String fieldReg, Object value, FilterType ft, 
			Class<?>... depsPath){
		if(depsPath.length == 0){
			return addFilter(fieldReg, value, ft, metaData.getEntityClz());
		}
		String field = getFieldByReg(fieldReg);
		checkField(depsPath[0], field);
		checkQueryPath(depsPath);
		if(!addedFilters.containsKey(depsPath[0])){
			addedFilters.put(depsPath[0], new HashMap<String, List<DynamicFilterDescriptor>>());
		}
		Map<String, List<DynamicFilterDescriptor>> fmps = addedFilters.get(depsPath[0]);
		if(!fmps.containsKey(field)){
			fmps.put(field, new ArrayList<DynamicFilterDescriptor>());
		}
		fmps.get(field).add(new DynamicFilterDescriptor(fieldReg, ft, value, !field.equals(fieldReg)));
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
	public NonQueryAdapter<T> addFilter(String fieldReg, Object value,
			Class<?>... depsPath) {
		return addFilter(fieldReg, value, FilterType.EQUAL, depsPath);
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
	public NonQueryAdapter<T> addNullFilter(String fieldReg, boolean isNull, Class<?>... depsPath) {
		return addFilter(fieldReg, null, isNull ? FilterType.IS : FilterType.IS_NOT, depsPath);
	}

	/**
	 * 
	 * <b>Description:	新增一个空过滤条件</b><br>
	 * @param fieldReg
	 * @param depsPath
	 * @return	
	 * 
	 */
	public NonQueryAdapter<T> addNullFilter(String fieldReg, Class<?>... depsPath) {
		return addNullFilter(fieldReg, true, depsPath);
	}
	
	@Override
	protected String getAliasByTableName(String tableName) {
		return tableName;
	}
	
	/**
	 * 
	 * <b>Description:	创建添加中间表数据的sql</b><br>
	 * @param data
	 * @param value
	 * @return
	 * 
	 */
	protected List<PreparedSqlDescriptor> createAddJoinRecordsSql(T data, Object value){
		List<PreparedSqlDescriptor> des = new ArrayList<>();
		for(JoinFieldMetaData<?> jfm : metaData.getJoinMetas()){
			if(!(jfm.getAnnotation() instanceof ManyToMany)){
				continue;
			}
			jfm.getField().setAccessible(true);
			List<?> jrs;
			try {
				jrs = (List<?>) jfm.getField().get(data);
			} catch (IllegalAccessException e) {
				throw new RabbitDMLException(e);
			}
			if(null == jrs || jrs.isEmpty()){
				continue;
			}
			PreparedSqlDescriptor psd = new PreparedSqlDescriptor(jrs.size());
			createAddJoinRecordsSql(value, jfm, jrs, psd);
			des.add(psd);
		}
		return des;
	}

	private void createAddJoinRecordsSql(Object value, JoinFieldMetaData<?> jfm, List<?> jrs,
			PreparedSqlDescriptor psd){
		for(Object o : jrs){
			StringBuilder rsql = new StringBuilder();
			List<PreparedValue> values = new ArrayList<>();
			Field jpk = MetaData.getPrimaryKeyField(jfm.getJoinClass());
			jpk.setAccessible(true);
			//子表的主键值
			Object jpkv = getValue(o, jpk);
			ManyToMany mtm = (ManyToMany) jfm.getAnnotation();
			rsql.append("INSERT INTO " + mtm.joinTable() + "(");
			rsql.append(mtm.joinColumn() + "," + mtm.reverseJoinColumn());
			if(!SessionFactory.isEmpty(mtm.id())){
				if(!mtm.policy().equals(Policy.AUTOINCREMENT)){
					rsql.append(", " + mtm.id());
				}
			}else{
				if(mtm.policy().equals(Policy.UUID) || mtm.policy().equals(Policy.SEQUENCE)){
					throw new RabbitDMLException("ManyToMany id must be specified when policy is [" + mtm.policy() + "]");
				}
			}
			FieldMetaData pkfmd = getPrimayKeyFieldMeta(metaData.getEntityClz());
            values.add(new PreparedValue(RabbitValueConverter.convert(value, pkfmd), pkfmd.getField()));
			FieldMetaData fmd = getPrimayKeyFieldMeta(jfm.getJoinClass());
            values.add(new PreparedValue(RabbitValueConverter.convert(jpkv, fmd), fmd.getField()));
			rsql.append(")VALUES(" + PLACE_HOLDER);
			rsql.append("," + PLACE_HOLDER);
			if(!SessionFactory.isEmpty(mtm.id())){
				if(mtm.policy().equals(Policy.UUID)){
					values.add(new PreparedValue(UUIDPolicy.getID(), jfm.getField()));
					rsql.append(", " + PLACE_HOLDER + ")");
				}
				if(mtm.policy().equals(Policy.SEQUENCE)){
					rsql.append(", " + mtm.sequence() + ".NEXTVAL)");
				}
				if(mtm.policy().equals(Policy.AUTOINCREMENT)){
					rsql.append(")");
				}
				
			}else{
				rsql.append(")");
			}
			psd.setSql(rsql);
			preparedValues.add(values);
		}
	}

    private Object getValue(Object o, Field jpk) {
        try {
        	return jpk.get(o);
        } catch (IllegalAccessException e) {
        	throw new RabbitDMLException(e);
        }
    }
	
	/**
	 * 
	 * <b>Description:	获取主键字段的描述符信息</b><br>
	 * @param clz
	 * @return	
	 * 
	 */
	protected FieldMetaData getPrimayKeyFieldMeta(Class<?> clz){
		for(FieldMetaData fmd : MetaData.getCachedFieldsMetas(clz)){
			if(fmd.isPrimaryKey()){
				return fmd;
			}
		}
		throw new RabbitDMLException("no primary key was found");
	}
	
	/**
	 * 
	 * <b>Description:	批量执行sql</b><br>
	 * @param conn
	 * @param psds
	 * @param counter
	 * @return
	 * @throws SQLException	
	 * 
	 */
	@SuppressWarnings("unchecked")
    protected int executeBatch(Connection conn, List<PreparedSqlDescriptor> psds, int counter) throws SQLException {
		for(PreparedSqlDescriptor psd : psds){
			PreparedStatement stmt = null;
			try{
			    stmt = conn.prepareStatement(psd.getSql().toString());
	            stmt.clearBatch();
	            sql = new StringBuilder("\n" + (sessionFactory.isFormatSql() ? SQLFormater.format(psd.getSql().toString()) : psd.getSql().toString()));
	            for(int i = 0; i < psd.getExecuteTimes(); i++){
	                List<PreparedValue> values = (List<PreparedValue>) preparedValues.get(counter);
	                counter++;
	                sql.append("\n");
	                sql.append("prepareStatement values(");
	                for(int j = 1; j <= values.size(); j++){
	                    setStmtValue(j, values.get(j - 1), stmt);
	                    sql.append(values.get(j - 1).getValue() + ", ");
	                }
	                sql.deleteCharAt(sql.lastIndexOf(","));
	                sql.deleteCharAt(sql.lastIndexOf(" "));
	                sql.append(")");
	                stmt.addBatch();
	            }
	            if(sessionFactory.isShowSql()){
	                logger.info(sql);
	            }
	            stmt.executeBatch();
			} finally {
			    clearBatch(stmt);
			    closeStmt(stmt);
			}
		}
		return counter;
	}

    private void clearBatch(PreparedStatement stmt){
        if(null != stmt){
            try {
                stmt.clearBatch();
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
	
	private void setStmtValue(int index, PreparedValue value, PreparedStatement stmt) throws SQLException{
		if(value.getValue() instanceof Date){
			stmt.setTimestamp(index, new Timestamp(((Date) value.getValue()).getTime()));
		}else{
			stmt.setObject(index, value.getValue());
		}
	}
	
	/**
	 * 
	 * <b>Description:	创建删除中间表记录的sql</b><br>
	 * @param data
	 * @param value
	 * @return
	 * 
	 */
	protected List<PreparedSqlDescriptor> createRemoveJoinRecordsSql(T data, Object value){
		List<PreparedSqlDescriptor> des = new ArrayList<>();
		for(JoinFieldMetaData<?> jfm : metaData.getJoinMetas()){
			if(!(jfm.getAnnotation() instanceof ManyToMany)){
				continue;
			}
			jfm.getField().setAccessible(true);
			List<?> jrs;
			try {
				jrs = (List<?>) jfm.getField().get(data);
			} catch (IllegalAccessException e) {
				throw new RabbitDMLException(e);
			}
			if(null == jrs || jrs.isEmpty()){
				continue;
			}
			PreparedSqlDescriptor psd = new PreparedSqlDescriptor(1);
			ManyToMany mtm = (ManyToMany) jfm.getAnnotation();
			StringBuilder rsql = new StringBuilder();
			rsql.append("DELETE FROM " + mtm.joinTable() + " WHERE ");
			FieldMetaData pkfmd = getPrimayKeyFieldMeta(metaData.getEntityClz());
            Object pv = RabbitValueConverter.convert(value, pkfmd);
			List<Object> values = new ArrayList<>();
			values.add(new PreparedValue(pv, pkfmd.getField()));
			rsql.append(mtm.joinColumn() + " = " + PLACE_HOLDER);
			rsql.append(" AND " + mtm.reverseJoinColumn() + " IN (");
			for(Object o : jrs){
				Field jpk = MetaData.getPrimaryKeyField(jfm.getJoinClass());
				jpk.setAccessible(true);
				//子表的主键值
				Object jpkv = getValue(o, jpk);
				FieldMetaData fmd = getPrimayKeyFieldMeta(jfm.getJoinClass());
                values.add(new PreparedValue(RabbitValueConverter.convert(jpkv, fmd), fmd.getField()));
				rsql.append("?,");
			}
			rsql.deleteCharAt(rsql.lastIndexOf(","));
			rsql.append(")");
			preparedValues.add(values);
			psd.setSql(rsql);
			des.add(psd);
		}
		return des;
	}
}

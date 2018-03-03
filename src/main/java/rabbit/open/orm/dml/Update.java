package rabbit.open.orm.dml;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.FilterType;
import rabbit.open.orm.dml.filter.DMLType;
import rabbit.open.orm.dml.filter.PreparedValue;
import rabbit.open.orm.dml.meta.FieldMetaData;
import rabbit.open.orm.dml.meta.FilterDescriptor;
import rabbit.open.orm.dml.meta.MetaData;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.pool.SessionFactory;
import rabbit.open.orm.shard.ShardFactor;

/**
 * <b>Description: 	更新操作</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public class Update<T> extends NonQueryAdapter<T>{

	private T value2Update = null;
	
	//动态添加的字段
	private Map<String, Object> settedValue = new HashMap<>();
		
	//设置为空的字段
	private HashSet<String> nullfields = new HashSet<>();
	
	private List<FieldMetaData> valueMetas;
	
	public Update(SessionFactory sessionFactory, Class<T> clz) {
        this(sessionFactory, null, clz);
    }
	
	public Update(SessionFactory sessionFactory, T filterData, Class<T> clz) {
		super(sessionFactory, filterData, clz);
		setDmlType(DMLType.UPDATE);
		sqlOperation = new SQLOperation() {

			@Override
			public long executeSQL(Connection conn) throws Exception {
			    PreparedStatement stmt = null;
				try {
	                sql.append(createUpdateSql(value2Update));
	                sql.append(createFilterSql());
	                replaceTableName();
	                showSql();
	                stmt = conn.prepareStatement(sql.toString());
	                setPreparedStatementValue(stmt, DMLType.UPDATE);
	                return stmt.executeUpdate();
				} finally {
				    closeStmt(stmt);
				}
			}

            private void replaceTableName() {
                String replaceAll = sql.toString().replaceAll(TABLE_NAME_REG, metaData.getTableName());
                sql = new StringBuilder(replaceAll);
            }
		};
	}
	
	@Override
	public long execute() {
	    prepareFilterMetas();
        combineFilters();
        doShardingCheck();
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
	public Update<T> addFilter(String fieldReg, Object value, FilterType ft, 
			Class<?>... depsPath) {
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
	public Update<T> addFilter(String fieldReg, Object value,
			Class<?>... depsPath) {
		super.addFilter(fieldReg, value, depsPath);
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
	public Update<T> addNullFilter(String reg, boolean isNull, Class<?>... depsPath) {
		 super.addFilter(reg, null, isNull ? FilterType.IS : FilterType.IS_NOT, depsPath);
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
	public Update<T> addNullFilter(String reg, Class<?>... depsPath) {
		 addNullFilter(reg, true, depsPath);
		 return this;
	}
	
	/**
	 * 
	 * <b>Description:	设置需要更新的字段的值</b><br>
	 * @param value
	 * @return
	 * 
	 */
	public Update<T> setValue(T value){
		this.value2Update = value;
		return this;
	}
	
	/**
	 * 
	 * <b>Description:	对单个字段设值</b><br>
	 * @param field
	 * @param value
	 * @return	
	 * 
	 */
	public Update<T> set(String field, Object value){
		if(null == value){
			return setNull(field);
		}else{
			settedValue.put(field, value);
		}
		return this;
	}
	
	/**
	 * 
	 * <b>Description:	将该字段的值更新成null</b><br>
	 * @param 	fields
	 * @return	
	 * 
	 */
	public Update<T> setNull(String... fields){
		for(String f : fields){
			nullfields.add(f);
		}
		return this;
	}
	
	/**
	 * 
	 * <b>Description:	根据id字段更新</b><br>
	 * @param 	data
	 * @return
	 * 
	 */
	public long updateByID(T data){
		sql = createUpdateSql(data);
		if(valueMetas.isEmpty()){
			throw new RabbitDMLException("no fields 2 update!");
		}
		Field pk = MetaData.getPrimaryKeyField(getEntityClz());
		pk.setAccessible(true);
		Object pkValue;
		pkValue = getValue(pk, data);
		if(null == pkValue){
			throw new RabbitDMLException("primary key can't be empty!");
		}
		preparedValues.add(new PreparedValue(RabbitValueConverter.convert(pkValue, new FieldMetaData(pk, pk.getAnnotation(Column.class))), pk));
		sql.append(" WHERE " + TARGET_TABLE_NAME + "." + metaData.getPrimaryKey() + " = " + PLACE_HOLDER);
		sqlOperation = new SQLOperation() {
			@Override
			public long executeSQL(Connection conn) throws Exception {
			    PreparedStatement stmt = null;
				try {
				    showSql();
	                stmt = conn.prepareStatement(sql.toString());
	                setPreparedStatementValue(stmt, DMLType.UPDATE);
	                return stmt.executeUpdate();
				} finally {
				    closeStmt(stmt);
				}
			}
		};
		factors.add(new ShardFactor(pk, FilterType.EQUAL.value(), pkValue));
		updateTargetTableName();
		return super.execute();
	}
	

	/**
	 * 
	 * <b>Description:	创建过滤条件的sql片段</b><br>
	 * @return
	 * 
	 */
	private StringBuilder createFilterSql() {
		StringBuilder sql = new StringBuilder();
		if(filterDescriptors.isEmpty()){
			return sql;
		}
		boolean isJoin = false;
		for(FilterDescriptor fd : filterDescriptors){
			if(fd.isJoinOn()){
				isJoin = true;
				break;
			}
		}
		if(isJoin){
			//联合更新
			sql.append(" WHERE " + metaData.getTableName() + "." + metaData.getPrimaryKey() + " IN "
					+ "(SELECT * FROM (");
			sql.append("SELECT " + metaData.getTableName() + "." + metaData.getPrimaryKey() 
					+ " FROM " + metaData.getTableName());
			sql.append(generateInnerJoinsql());
			sql.append(generateFilterSql());
			sql.append(")t)");
		}else{
			sql.append(generateFilterSql());
		}
		return sql;
	}
	
	/**
	 * 
	 * <b>Description:	创建update部分的sql</b><br>
	 * @param valueData
	 * @return
	 * 
	 */
	private StringBuilder createUpdateSql(T valueData) {
		StringBuilder sql = new StringBuilder();
		valueMetas = getNonEmptyColumnFieldMetas(valueData);
		if(valueMetas.isEmpty()){
			throw new RabbitDMLException("no field is expected to update!");
		}
		sql.append("UPDATE " + TARGET_TABLE_NAME + " SET");
		int fields2Update = 0;
		for(int i = 0; i < valueMetas.size(); i++){
			FieldMetaData fmd = valueMetas.get(i);
			if(fmd.isPrimaryKey() && (sessionFactory.getDialectType().isSQLServer() 
			        || sessionFactory.getDialectType().isDB2())){
				//sqlserver的主键是不能被更新的
			    //DB2不更新主键
				continue;
			}
			if(null == fmd.getFieldValue()){
				preparedValues.add(new PreparedValue(null));
				fields2Update++;
				sql.append(" " + metaData.getTableName() + "." + fmd.getColumn().value() + " = " + PLACE_HOLDER + ", ");
				continue;
			}
			if(fmd.isForeignKey()){
				appendForeignKeyValue(sql, fmd);
			}else{
				appendCommonFieldsValue(sql, fmd);
			}
			fields2Update++;
		}
		if(0 == fields2Update){
		    throw new RabbitDMLException("no fields 2 update");
		}
		sql.deleteCharAt(sql.lastIndexOf(","));
		return sql;
	}

	/**
	 * 
	 * <b>Description:	提取普通字段的值并添加到sql中</b><br>
	 * @param sql
	 * @param fmd	
	 * 
	 */
	private void appendCommonFieldsValue(StringBuilder sql, FieldMetaData fmd) {
		preparedValues.add(new PreparedValue(RabbitValueConverter.convert(fmd.getFieldValue(), fmd), fmd.getField()));
		sql.append(" " + TARGET_TABLE_NAME + "." + fmd.getColumn().value() + "=");
		sql.append(PLACE_HOLDER);
		sql.append(",");
	}

	/**
	 * 
	 * <b>Description:	提取外键值并添加到sql中</b><br>
	 * @param sql
	 * @param fmd	
	 * 
	 */
	private void appendForeignKeyValue(StringBuilder sql, FieldMetaData fmd) {
		try {
			Field foreignField = fmd.getForeignField();
			foreignField.setAccessible(true);
			Object fkValue = foreignField.get(fmd.getFieldValue());
			if(null != fkValue){
				preparedValues.add(new PreparedValue(RabbitValueConverter.convert(fkValue, new FieldMetaData(foreignField, 
                        foreignField.getAnnotation(Column.class))), foreignField));
				sql.append(" " + TARGET_TABLE_NAME + "." + fmd.getColumn().value() + "=");
				sql.append(PLACE_HOLDER);
				sql.append(",");
			}
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * 
	 * 获取条件对象中有值的mapping字段映射信息
	 * @param data
	 * @return
	 * 
	 */
	private List<FieldMetaData> getNonEmptyColumnFieldMetas(Object data){
		data = combineValues(data);
		if(null == data){
			return new ArrayList<>();
		}
		String tableName = getTableNameByClass(data.getClass());
		Class<?> clz = data.getClass();
		List<FieldMetaData> fields = new ArrayList<>();
		while(!clz.equals(Object.class)){
			for(Field f : clz.getDeclaredFields()){
				Column col = f.getAnnotation(Column.class);
				if(null == col){
					continue;
				}
				if(nullfields.contains(f.getName())){
					fields.add(new FieldMetaData(f, col, null, tableName));
					continue;
				}
				Object fieldValue = null;
				f.setAccessible(true);
				try {
					fieldValue = f.get(data);
				} catch (Exception e) {
					continue;
				}
				if(null == fieldValue){
					continue;
				}
				fields.add(new FieldMetaData(f, col, fieldValue, tableName));
			}
			clz = clz.getSuperclass();
		}
		return fields;
	}
	
	/**
	 * 
	 * <b>Description:	合并需要更新的值</b><br>
	 * @param data
	 * @return
	 * 
	 */
	private Object combineValues(Object data) {
		if(nullfields.isEmpty() && settedValue.isEmpty()){
			return data;
		}
		if(data == null){
			data = constructData();
		}
		Iterator<String> it = settedValue.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			FieldMetaData fmd = MetaData.getCachedFieldsMeta(getEntityClz(), key);
			fmd.getField().setAccessible(true);
			Object value = settedValue.get(key);
            try{
				fmd.getField().set(data, value);
			}catch(Exception e){
				setEntityFiled(data, key, fmd, value);
			}
		}
		return data;
	}

    /**
     * <b>Description  将简单字段封装成复杂对象.</b>
     * @param data
     * @param key
     * @param fmd
     * @param value
     */
    private void setEntityFiled(Object data, String key, FieldMetaData fmd,
            Object value) {
        Field pk = null;
        pk = MetaData.getPrimaryKeyField(fmd.getField().getType());
        logger.warn("value[" + value + "] is not compatible with field[" 
        		+ key + "(" + fmd.getField().getType().getName() + ")] of " 
        		+ data.getClass().getName());
        pk.setAccessible(true);
        try{
        	Object bean = fmd.getField().getType().getDeclaredConstructor().newInstance();
        	if(value instanceof Number){
        	    value = RabbitValueConverter.cast(new BigDecimal(value.toString()), pk.getType());
        	}
        	pk.set(bean, value);
        	fmd.getField().set(data, bean);
        }catch(Exception ee){
        	throw new RabbitDMLException(ee.getMessage(), ee);
        }
    }

	/**
	 * 
	 * <b>Description:	反射构造一个data</b><br>
	 * @return	
	 * 
	 */
	private Object constructData() {
		try {
			return getEntityClz().getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RabbitDMLException(e.getMessage(), e);
		}
	}
}

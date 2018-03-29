package rabbit.open.orm.dml;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import rabbit.open.orm.dml.meta.MultiDropFilter;
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
            public long executeSQL(Connection conn) throws SQLException {
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
		};
	}

	private void replaceTableName() {
	    String replaceAll = sql.toString().replaceAll(TABLE_NAME_REG, metaData.getTableName());
	    sql = new StringBuilder(replaceAll);
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
     * <b>Description  添加Or类型的过滤条件</b>
     * @param multiDropFilter
     */
    public Update<T> setMultiDropFilter(MultiDropFilter multiDropFilter) {
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
	 * <b>Description  获取对象更新器</b>
	 * @return
	 */
	public T getUpdater() {
	    if (null == this.value2Update) {
	        this.value2Update = DMLAdapter.newInstance(getEntityClz());
	    }
	    return this.value2Update;
	}

	/**
	 * 
	 * <b>Description:	对单个字段设值</b><br>
	 * @param field
	 * @param value
	 * @return	
	 * 
	 */
    public Update<T> set(String field, Object value) {
        if (null == value) {
            return setNull(field);
        } else {
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
    public Update<T> setNull(String... fields) {
        for (String f : fields) {
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
        Field pk = MetaData.getPrimaryKeyField(getEntityClz());
        Object pkValue;
        pkValue = getValue(pk, data);
        if (null == pkValue) {
            throw new RabbitDMLException("primary key can't be empty!");
        }
		preparedValues.add(new PreparedValue(RabbitValueConverter.convert(pkValue, new FieldMetaData(pk, pk.getAnnotation(Column.class))), pk));
		sql.append(WHERE + TARGET_TABLE_NAME + "." + getColumnName(metaData.getPrimaryKey()) + " = " + PLACE_HOLDER);
        sqlOperation = new SQLOperation() {
            @Override
            public long executeSQL(Connection conn) throws SQLException {
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
        if (filterDescriptors.isEmpty()) {
            StringBuilder mds = createMultiDropSql();
            if (0 != mds.length()) {
                mds.insert(0, WHERE);
                sql.append(mds);
            }
            return sql;
        }
        boolean isJoin = false;
        for (FilterDescriptor fd : filterDescriptors) {
            if (fd.isJoinOn()) {
                isJoin = true;
                break;
            }
        }
        if (isJoin) {
            // 联合更新
            sql.append(WHERE + metaData.getTableName() + "."
                    + getColumnName(metaData.getPrimaryKey()) + " IN "
                    + "(SELECT * FROM (");
            sql.append("SELECT " + metaData.getTableName() + "."
                    + getColumnName(metaData.getPrimaryKey()) + " FROM "
                    + metaData.getTableName());
            sql.append(generateInnerJoinsql());
            sql.append(generateFilterSql());
            sql.append(")t)");
        } else {
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
        StringBuilder sb = new StringBuilder();
        valueMetas = getNonEmptyFieldMetas(valueData);
        if (noFields2Update()) {
            throw new RabbitDMLException("no field is expected to update!");
        }
        sb.append("UPDATE " + TARGET_TABLE_NAME + " SET");
        for (int i = 0; i < valueMetas.size(); i++) {
            sb.append(createSqlSegmentByMeta(valueMetas.get(i)));
        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        return sb;
	}

    private StringBuilder createSqlSegmentByMeta(FieldMetaData fmd) {
        StringBuilder sb = new StringBuilder();
        if (fmd.isPrimaryKey()) {
            return sb;
        }
        if (null == fmd.getFieldValue()) {
            preparedValues.add(new PreparedValue(null));
            sb.append(createFieldSqlPiece(getColumnName(fmd.getColumn())));
            return sb;
        }
        if (fmd.isForeignKey()) {
            sb.append(createForeignKeyValueSqlSegment(fmd));
        } else {
            sb.append(createCommonFieldsValueSegment(fmd));
        }
        return sb;
    }

    private boolean noFields2Update() {
        return valueMetas.isEmpty() || (1 == valueMetas.size() && valueMetas.get(0).isPrimaryKey());
    }

	/**
	 * 
	 * <b>Description:	提取普通字段的值并添加到sql中</b><br>
	 * @param fmd	
	 * 
	 */
	private StringBuilder createCommonFieldsValueSegment(FieldMetaData fmd) {
	    StringBuilder sb =  new StringBuilder();
		preparedValues.add(new PreparedValue(RabbitValueConverter.convert(fmd.getFieldValue(), fmd), 
		        fmd.getField()));
		sb.append(createFieldSqlPiece(getColumnName(fmd.getColumn())));
		return sb;
	}

    /**
     * <b>Description  创建更新字段sql片段</b>
     * @param columnName
     * @return
     */
    private StringBuilder createFieldSqlPiece(String columnName) {
        StringBuilder sb = new StringBuilder();
        if (sessionFactory.getDialectType().isSQLITE3()) {
            sb.append(" " + columnName + " = ");
        } else {
            sb.append(" " + TARGET_TABLE_NAME + "." + columnName + " = ");
        }
		sb.append(PLACE_HOLDER);
		sb.append(",");
		return sb;
    }

	/**
	 * 
	 * <b>Description:	提取外键值并添加到sql中</b><br>
	 * @param fmd	
	 * 
	 */
	private StringBuilder createForeignKeyValueSqlSegment(FieldMetaData fmd) {
	    StringBuilder sql = new StringBuilder();
        Field foreignField = fmd.getForeignField();
        Object fkValue = getValue(foreignField, fmd.getFieldValue());
        if (null != fkValue) {
            preparedValues.add(new PreparedValue(RabbitValueConverter.convert(
                    fkValue, new FieldMetaData(foreignField, foreignField
                            .getAnnotation(Column.class))), foreignField));
            sql.append(createFieldSqlPiece(getColumnName(fmd.getColumn())));
        }
        return sql;
	}
	
	/**
	 * 
	 * 获取条件对象中有值的mapping字段映射信息
	 * @param data
	 * @return
	 * 
	 */
    private List<FieldMetaData> getNonEmptyFieldMetas(Object data) {
        data = combineValues(data);
        if (null == data) {
            return new ArrayList<>();
        }
        String tableName = getTableNameByClass(data.getClass());
        Class<?> clz = data.getClass();
        List<FieldMetaData> fields = new ArrayList<>();
        while (!clz.equals(Object.class)) {
            for (Field field : clz.getDeclaredFields()) {
                fields.addAll(getNonEmptyFieldMetasByField(data,
                        tableName, field));
            }
            clz = clz.getSuperclass();
        }
        return fields;
    }

    private List<FieldMetaData> getNonEmptyFieldMetasByField(Object data,
            String tableName, Field field) {
        List<FieldMetaData> fields = new ArrayList<>();
        Column col = field.getAnnotation(Column.class);
        if (null == col) {
            return fields;
        }
        if (nullfields.contains(field.getName())) {
            fields.add(new FieldMetaData(field, col, null, tableName));
            return fields;
        }
        Object fieldValue = getValue(field, data);
        if (null == fieldValue) {
            return fields;
        }
        fields.add(new FieldMetaData(field, col, fieldValue, tableName));
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
        if (nullfields.isEmpty() && settedValue.isEmpty()) {
            return data;
        }
        if (data == null) {
            data = DMLAdapter.newInstance(getEntityClz());
        }
        Iterator<String> it = settedValue.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            FieldMetaData fmd = MetaData.getCachedFieldsMeta(getEntityClz(),
                    key);
            Object value = settedValue.get(key);
            try {
                setValue2Field(data, fmd.getField(), value);
            } catch (Exception e) {
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
        Field pk = MetaData.getPrimaryKeyField(fmd.getField().getType());
        logger.warn("value[" + value + "] is not compatible with field[" 
        		+ key + "(" + fmd.getField().getType().getName() + ")] of " 
        		+ data.getClass().getName());
        Object bean = DMLAdapter.newInstance(fmd.getField().getType());
        if(value instanceof Number){
            value = RabbitValueConverter.cast(new BigDecimal(value.toString()), pk.getType());
        }
        setValue2Field(bean, pk, value);
        setValue2Field(data, fmd.getField(), bean);
    }

}

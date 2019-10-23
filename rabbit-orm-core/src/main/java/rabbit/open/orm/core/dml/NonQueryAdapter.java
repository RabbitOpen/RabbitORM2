package rabbit.open.orm.core.dml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.common.exception.EmptyPrimaryKeyValueException;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.common.exception.UnKnownFieldException;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.FilterDescriptor;
import rabbit.open.orm.core.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.PreparedSqlDescriptor;
import rabbit.open.orm.core.dml.policy.UUIDPolicy;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.utils.SQLFormater;
import rabbit.open.orm.datasource.Session;

/**
 * <b>Description: 	非查询操作的适配器</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public abstract class NonQueryAdapter<T> extends DMLObject<T> {

	protected SQLOperation sqlOperation;
	
	protected static final String TABLE_NAME_REG = "\\#\\{TARGETTABLENAME\\}";

	protected static final String TARGET_TABLE_NAME = "#{TARGETTABLENAME}";
	
	private DMLType dmlType;
	
	public NonQueryAdapter(SessionFactory sessionFactory, Class<T> clz) {
		super(sessionFactory, clz);
	}

	public NonQueryAdapter(SessionFactory sessionFactory, T filterData,
			Class<T> clz) {
		super(sessionFactory, filterData, clz);
	}

    protected interface SQLOperation {
		
		/**
		 * 
		 * <b>Description:	执行sql操作</b><br>
		 * @param conn
		 * @throws Exception	
		 * 
		 */
		public long executeSQL(Connection conn) throws SQLException;
	}
	
	/**
     * <b>Description  设置当前操作的目标表的名字</b>
     */
    protected void updateTargetTableName() {
        if (isShardingOperation()) {
            String tableName = getCurrentShardedTableName(getFactors());
            String replaceAll = sql.toString().replaceAll(TABLE_NAME_REG, tableName);
            sql = new StringBuilder(replaceAll);
        } else {
            sql = new StringBuilder(sql.toString().replaceAll(TABLE_NAME_REG, metaData.getTableName()));
        }
    }
    
    public void setDmlType(DMLType dmlType) {
        this.dmlType = dmlType;
    }
    
    protected void doShardingCheck() {
        if (!isShardingOperation()) {
            return;
        }
        List<FilterDescriptor> mfds = getMainFilterDescriptors();
        for (FilterDescriptor fd : mfds) {
            factors.add(new ShardFactor(fd.getField(), fd.getFilter(), fd.getValue()));
        }
        metaData.updateTableName(getCurrentShardedTableName(factors));
        dependencyPath.clear();
        addedFilters.clear();
        clzesEnabled2Join = null;
        filterDescriptors.clear();
        runCallBackTask();
        prepareFilterMetas();
        mergeFilters();
    }
    
    @Override
    protected List<ShardFactor> getFactors() {
        return factors;
    }
	
	/**
	 * 
	 * <b>Description:	执行sql</b><br>
	 * @return
	 * 
	 */
	public long execute() {
		Connection conn = null;
		try {
			conn = sessionFactory.getConnection(getEntityClz(), getCurrentTableName(), dmlType);
			return sqlOperation.executeSQL(conn);
		} catch (UnKnownFieldException e) {
			throw e;
		} catch (Exception e) {
			showUnMaskedSql(false);
			Session.flagException();
			throw new RabbitDMLException(e.getMessage(), e);
		} finally {
			closeConnection(conn);
			Session.clearException();
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
    public NonQueryAdapter<T> addFilter(String fieldReg, Object value,
            FilterType ft, Class<?>... depsPath) {
        if (depsPath.length == 0) {
            return addFilter(fieldReg, value, ft, getEntityClz());
        }
        filterTasks.add(() -> {
        	String field = getFieldByReg(fieldReg);
            checkField(depsPath[0], field);
            checkQueryPath(depsPath);
            if (!addedFilters.containsKey(depsPath[0])) {
                addedFilters.put(depsPath[0], new HashMap<>());
            }
            Map<String, List<DynamicFilterDescriptor>> fmps = addedFilters.get(depsPath[0]);
            if (!fmps.containsKey(field)) {
                fmps.put(field, new ArrayList<>());
            }
            fmps.get(field).add(new DynamicFilterDescriptor(fieldReg, ft, value, !field.equals(fieldReg)));
        });
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
	

	/**
	 * 
	 * <b>Description:	新增一个非空过滤条件</b><br>
	 * @param fieldReg
	 * @param depsPath
	 * @return	
	 * 
	 */
	public NonQueryAdapter<T> addNotNullFilter(String fieldReg, Class<?>... depsPath) {
		return addFilter(fieldReg, null, FilterType.IS_NOT, depsPath);
	}
	
	
	@Override
	protected String getAliasByTableName(String tableName) {
		return tableName;
	}
	
    /**
     * <b>@description 根据JoinFieldMetaData创建sql </b>
     * @param data
     * @param jfm
     * @return
     */
	protected PreparedSqlDescriptor createAddJoinRecordsSql(T data, JoinFieldMetaData<?> jfm) {
		List<?> joinRecords = getM2MJoinFieldValue(data, jfm);
		if (null == joinRecords || joinRecords.isEmpty()) {
		    return null;
		}
		Object value = getValue(jfm.getMasterField(), data);
		assertEmptyPKValue(value);
		return createAddJoinRecordsSql(value, jfm, joinRecords);
	}
    
    protected void assertEmptyPKValue(Object value) {
        if (null == value) {
            throw new EmptyPrimaryKeyValueException();
        }
    }
    
    @SuppressWarnings("rawtypes")
    protected List getM2MJoinFieldValue(T data, JoinFieldMetaData<?> jfm) {
        if (!(jfm.getAnnotation() instanceof ManyToMany)) {
            return new ArrayList<>();
        }
        return (List) getValue(jfm.getField(), data);
    }

    /**
     * <b>@description 创建添加中间表记录的sql </b>
     * @param value	主表主键字段的值
     * @param jfm	关联信息
     * @param joinRecords	关联表对象值
     * @return
     */
    private PreparedSqlDescriptor createAddJoinRecordsSql(Object value, JoinFieldMetaData<?> jfm, List<?> joinRecords) {
        PreparedSqlDescriptor psd = new PreparedSqlDescriptor(joinRecords.size());
        for (Object record : joinRecords) {
            StringBuilder rsql = new StringBuilder();
            List<PreparedValue> values = new ArrayList<>();
            ManyToMany mtm = (ManyToMany) jfm.getAnnotation();
            rsql.append("INSERT INTO " + mtm.joinTable() + "(");
            rsql.append(mtm.joinColumn() + "," + mtm.reverseJoinColumn());
            appendIdField(rsql, mtm);
            // 子表的关联键值
            FieldMetaData pkfmd = MetaData.getCachedFieldsMeta(getEntityClz(), jfm.getMasterField().getName());
            values.add(new PreparedValue(RabbitValueConverter.convert(value, pkfmd), pkfmd.getField()));
            FieldMetaData fmd = MetaData.getCachedFieldsMeta(jfm.getJoinClass(), jfm.getSlaveField().getName());
            values.add(new PreparedValue(RabbitValueConverter.convert(getValue(jfm.getSlaveField(), record), fmd), fmd.getField()));
            rsql.append(")VALUES(" + PLACE_HOLDER);
            rsql.append("," + PLACE_HOLDER);
            appendIdFieldValue(jfm, rsql, values, mtm);
            psd.setSql(rsql);
            preparedValues.add(values);
        }
        return psd;
    }

 // 生成多对多中间表时，向insert语句添加当前这条记录的主键id字段的值信息
	private void appendIdFieldValue(JoinFieldMetaData<?> jfm, StringBuilder rsql, List<PreparedValue> values,
			ManyToMany mtm) {
		if (!SessionFactory.isEmpty(mtm.id())) {
		    if (mtm.policy().equals(Policy.UUID)) {
		        values.add(new PreparedValue(UUIDPolicy.getID(), jfm.getField()));
		        rsql.append(", " + PLACE_HOLDER + ")");
		    }
		    if (mtm.policy().equals(Policy.SEQUENCE)) {
		        rsql.append(", " + mtm.sequence() + ".NEXTVAL)");
		    }
		    if (mtm.policy().equals(Policy.AUTOINCREMENT)) {
		        rsql.append(")");
		    }
		} else {
		    rsql.append(")");
		}
	}

	// 生成多对多中间表时，向insert语句添加当前这条记录的主键id字段信息
	private void appendIdField(StringBuilder rsql, ManyToMany mtm) {
		if (!SessionFactory.isEmpty(mtm.id())) {
		    if (!mtm.policy().equals(Policy.AUTOINCREMENT)) {
		        rsql.append(", " + mtm.id());
		    }
		} else {
		    if (mtm.policy().equals(Policy.UUID) || mtm.policy().equals(Policy.SEQUENCE)) {
		        throw new RabbitDMLException("ManyToMany id must be specified when policy is [" + mtm.policy() + "]");
		    }
		}
	}

    /**
	 * 
	 * <b>Description:	批量执行sql</b><br>
	 * @param conn
	 * @param psds
	 * @throws SQLException	
	 * 
	 */
    protected void executeBatch(Connection conn, List<PreparedSqlDescriptor> psds) 
            throws SQLException {
	    for (PreparedSqlDescriptor psd : psds) {
            executeOneByOne(conn, psd);
        }
	}

    @SuppressWarnings("unchecked")
    private void executeOneByOne(Connection conn, PreparedSqlDescriptor psd)
            throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(psd.getSql().toString());
            stmt.clearBatch();
            StringBuilder sql = new StringBuilder("\n" + (sessionFactory.isFormatSql() ? 
                    SQLFormater.format(psd.getSql().toString()) : psd.getSql().toString()));
            for (int i = 0; i < psd.getExecuteTimes(); i++) {
                List<PreparedValue> values = (List<PreparedValue>) preparedValues.get();
                sql.append("\n");
                sql.append("prepareStatement values(");
                for (int j = 1; j <= values.size(); j++) {
                    setStmtValue(j, values.get(j - 1), stmt);
                    sql.append(values.get(j - 1).getValue() + ", ");
                }
                sql.deleteCharAt(sql.lastIndexOf(","));
                sql.deleteCharAt(sql.lastIndexOf(" "));
                sql.append(")");
                stmt.addBatch();
            }
            if (sessionFactory.isShowSql()) {
                logger.info("{}", sql);
            }
            stmt.executeBatch();
        } finally {
            clearBatch(stmt);
            closeStmt(stmt);
        }
    }

    private void clearBatch(PreparedStatement stmt) {
        if (null != stmt) {
            try {
                stmt.clearBatch();
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void setStmtValue(int index, PreparedValue value,
            PreparedStatement stmt) throws SQLException {
        if (value.getValue() instanceof Date) {
            stmt.setTimestamp(index, new Timestamp(((Date) value.getValue()).getTime()));
        } else {
            stmt.setObject(index, value.getValue());
        }
    }

}

package rabbit.open.orm.core.dml;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.AmbiguousDependencyException;
import rabbit.open.orm.common.exception.CycleDependencyException;
import rabbit.open.orm.common.exception.InvalidFetchOperationException;
import rabbit.open.orm.common.exception.InvalidJoinFetchOperationException;
import rabbit.open.orm.common.exception.InvalidQueryPathException;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.common.exception.UnKnownFieldException;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.dml.filter.DMLFilter;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.FilterDescriptor;
import rabbit.open.orm.core.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.MultiDropFilter;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.name.NamedSQL;
import rabbit.open.orm.core.dml.shard.DefaultShardingPolicy;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.ShardingPolicy;
import rabbit.open.orm.core.utils.SQLFormater;

/**
 * <b>Description: 	所有dml操作的基类</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public abstract class DMLObject<T> {

    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    protected static final String NULL = " NULL ";
    
    protected static final String WHERE = " WHERE ";
    
    // 禁止动态字段的查询
    protected boolean forbiddenDynamic = true;

    //换行
	private static final String NEW_LINE = "\n";

    protected static final String UNDERLINE = "_";

    Logger logger = LoggerFactory.getLogger(getClass());
	
	protected List<CallBackTask> filterTasks;
	
	protected SessionFactory sessionFactory;
	
	protected static final String SEPARATOR = "$";
	
	protected PreparedValueList<Object> preparedValues = new PreparedValueList<>();
	
	protected static final String PLACE_HOLDER = "?";
	
	//字段替换符号"${}"的正则表达式
	protected static final String REPLACE_WORD = "\\$\\{(.*?)\\}";
	
	// 命名sql对象
	protected NamedSQL namedObject;

	protected TreeMap<Integer, PreparedValue> fieldsValues = new TreeMap<>();
	
	//sql语句
	protected StringBuilder sql = new StringBuilder();
	
	//过滤条件
	protected T filterData;
	
	protected MetaData<T> metaData;
	
	//where关键字后面的过滤条件
	protected List<FilterDescriptor> filterDescriptors = new ArrayList<>();
	
	//能够被当前表对应的class fetch(many2one)的class
	protected Map<Class<?>, List<FilterDescriptor>> clzesEnabled2Join;
	
	protected Pattern pattern;
	
	protected List<MultiDropFilter> multiDropFilters = new ArrayList<>();
	
	//动态添加的过滤器
	protected Map<Class<?>, Map<String, List<DynamicFilterDescriptor>>> addedFilters;

	//动态添加的left join过滤器
	protected Map<Class<?>, Map<String, List<DynamicFilterDescriptor>>> addedJoinFilters;

	//动态添加的DMLFilter
	protected Map<Class<?>, DMLFilter> dmlFilters = new HashMap<>();
	
	//缓存class的依赖路径, 同一个clz不能有多个路径
	protected Map<Class<?>, List<FilterDescriptor>> dependencyPath = new HashMap<>();
	
	//分片表分片时的因子
	protected List<ShardFactor> factors = new ArrayList<>();

	public DMLObject() {}
	
	public DMLObject(SessionFactory sessionFactory, Class<T> clz) {
		this(sessionFactory, null, clz);
	}

	@SuppressWarnings("unchecked")
    public DMLObject(SessionFactory sessionFactory, T filterData, Class<T> clz) {
		this.sessionFactory = sessionFactory;
		this.filterData = filterData;
		metaData = (MetaData<T>) MetaData.getMetaByClass(clz);
		pattern  = Pattern.compile(REPLACE_WORD);
		addedFilters = new HashMap<>();
		addedJoinFilters = new HashMap<>();
		filterTasks = new ArrayList<>();
	}
	
	public abstract List<ShardFactor> getFactors();
 	
	/**
	 * 
	 * <b>Description:	打印sql</b><br>	
	 * 
	 */
	public void showSql() {
		if (null == sql || 0 == sql.length() || !sessionFactory.isShowSql()) {
			return;
		}
		if (sessionFactory.isMaskPreparedSql()) {
			showMaskedPreparedSql();
		} else {
			showUnMaskedSql();
		}
	}

	public Class<T> getEntityClz() {
        return metaData.getEntityClz();
    }
	
    /**
     * <b>Description  显示带问号的sql.</b>
     */
    public void showMaskedPreparedSql() {
        Object formattedSql = sessionFactory.isFormatSql() ? SQLFormater.format(sql.toString()) : sql.toString();
		logger.info("{}{}", NEW_LINE, formattedSql);
    }

    /**
     * <b>Description  显示带真实值的sql.</b>
     */
    public void showUnMaskedSql() {
        showUnMaskedSql(true);
    }

	/**
	 * <b>@description  显示带真实值的sql</b>
	 * @param info 是否以info级别打印日志
	 */
	protected void showUnMaskedSql(boolean info) {
		try {
            String valuesql = sql.toString();
            StringBuilder vs = new StringBuilder("prepareStatement values(");
            for (Object v : preparedValues) {
                Object text = convert2Str((PreparedValue) v);
                valuesql = replace(valuesql, text.toString());
                vs.append(text + ", ");
            }
            if (-1 != vs.indexOf(",")) {
                int index = vs.lastIndexOf(",");
                vs.deleteCharAt(index);
                vs.deleteCharAt(index);
            }
            vs.append(")");
            String msg = NEW_LINE + (sessionFactory.isFormatSql() ? SQLFormater
					.format(valuesql) : valuesql)
					+ (preparedValues.isEmpty() ? "" : (NEW_LINE + vs
							.toString()));
			if (info) {
            	logger.info("{}", msg);
            } else {
            	logger.error("{}", msg);
            }
        } catch (Exception e) {
            logger.error("show sql error for " + e.getMessage(), e);
        }
	}

	public StringBuilder getSql() {
		return sql;
	}
	
    /**
     * 
     * <b>Description:    转成字符串</b><br>.
     * @param v
     * @return	
     * 
     */
	private Object convert2Str(PreparedValue v) {
		if (null == v || null == v.getValue()) {
			return "null";
		}
		if (v.getValue() instanceof String) {
			return "'" + v.getValue().toString().replaceAll("'", "\\\\'") + "'";
		}
		if (v.getValue() instanceof Enum) {
			return "'" + v.getValue().toString().replaceAll("'", "\\\\'") + "'";
		}
		if (v.getValue() instanceof Date) {
			String ds = new SimpleDateFormat(DEFAULT_DATE_PATTERN).format(v.getValue());
			if (sessionFactory.getDialectType().isOracle()) {
				return "to_date('" + ds + "','YYYY-MM-DD HH24:MI:SS')";
			} else {
				return "'" + ds + "'";
			}
		}
		return v.getValue();
	}
	
	private String replace(String src, String replace) {
		StringBuilder sb = new StringBuilder(src);
		int index = sb.indexOf("?");
		sb.deleteCharAt(index);
		sb.insert(index, replace);
		return sb.toString();
	}
	
	/**
	 * 
	 * <b>Description:	为jdbc存储过程设值</b><br>
	 * @param 	stmt
	 * @throws 	SQLException	
	 * 
	 */
	@SuppressWarnings("rawtypes")
	protected void setPreparedStatementValue(PreparedStatement stmt, DMLType dmlType) throws SQLException {
		for (int i = 1; i <= preparedValues.size(); i++) {
			PreparedValue pv = (PreparedValue) preparedValues.get(i - 1);
			Object value = sessionFactory.onValueSet(pv, dmlType);
			pv.setValue(value);
			if (value instanceof Date) {
				setDate(stmt, i, (Date) value);
			} else if (value instanceof Double) {
				stmt.setDouble(i, (double) value);
			} else if (value instanceof Float) {
				stmt.setFloat(i, (float) value);
			} else if (value instanceof Enum) {
				stmt.setString(i, ((Enum) value).name());
			} else if (value instanceof byte[]) {
				stmt.setBytes(i, (byte[]) value);
			} else {
				stmt.setObject(i, value);
			}
		}
	}

    private void setDate(PreparedStatement stmt, int i, Date date)
            throws SQLException {
        if (sessionFactory.getDialectType().isSQLITE3()) {
            stmt.setString(i, new SimpleDateFormat(DEFAULT_DATE_PATTERN).format(date));
        } else {
            stmt.setTimestamp(i, new Timestamp(date.getTime()));
        }
    }
	
	/**
	 * 
	 * 准备过滤的filterDescriptors信息
	 */
	protected void prepareFilterMetas() {
		if (this.filterData != null) {
			generateFilters(getNonEmptyFieldMetas(this.filterData, getEntityClz()));
		}
	}
	
	/**
	 * 
	 * <b>Description:	执行动态回调任务</b><br>	
	 * 
	 */
	protected void runCallBackTask() {
		if (filterTasks.isEmpty()) {
			return;
		}
		for (CallBackTask dft : filterTasks) {
			dft.run();
		}
	}
	
	/**
	 * 
	 * 获取条件对象中有值的字段映射信息
	 * @param 	data
	 * @param 	clz
	 * 
	 */
	protected final List<FieldMetaData> getNonEmptyFieldMetas(Object data, Class<?> clz) {
        if (null == data) {
            return new ArrayList<>();
        }
        String tableName = getTableNameByClass(clz);
        List<FieldMetaData> fields = new ArrayList<>();
        Class<?> clzSuper = clz;
        while (!clzSuper.equals(Object.class)) {
            for (Field f : clzSuper.getDeclaredFields()) {
                Column col = f.getAnnotation(Column.class);
                Object fieldValue = getValue(f, data);
                if (null == col || null == fieldValue) {
                    continue;
                }
                fields.add(new FieldMetaData(f, col, fieldValue, tableName));
            }
            clzSuper = clzSuper.getSuperclass();
        }
		return fields;
	}
	
	/**
     * 
     * <b>Description:  反射取值</b><br>
     * @param field
     * @param target
     * @return  
     * 
     */
	protected Object getValue(Field field, Object target) {
		try {
			field.setAccessible(true);
			return field.get(target);
		} catch (Exception e) {
			throw new RabbitDMLException(e.getMessage());
		}
	}
	
	//生成过滤条件
	protected void generateFilters(List<FieldMetaData> fieldMetas) {
        for (FieldMetaData fmd : fieldMetas) {
            if (fmd.isForeignKey()) {
                String fkName = getManyToOnePrimaryKeyColumnName(fmd);
                String fkTable = getTableNameByClass(fmd.getField().getType());
                MetaData.updateTableMapping(fkTable, fmd.getField().getType());
                FilterDescriptor desc = new FilterDescriptor(
                        getAliasByTableName(fmd.getFieldTableName()) + "."
                                + getColumnName(fmd.getColumn()),
                        getAliasByTableName(fkTable) + "." + fkName);
                desc.setField(fmd.getField());
                desc.setJoinOn(true);
                desc.setFilterTable(fkTable);
                desc.setJoinField(fmd.getField());
                filterDescriptors.add(desc);
                generateFilters(getNonEmptyFieldMetas(fmd.getFieldValue(), fmd.getField().getType()));
            } else {
                FilterDescriptor desc = new FilterDescriptor(
                        getAliasByTableName(fmd.getFieldTableName()) + "."
                                + getColumnName(fmd.getColumn()),
                        RabbitValueConverter.convert(fmd.getFieldValue(), fmd),
                        FilterType.EQUAL.value());
                desc.setField(fmd.getField());
                desc.setFilterTable(fmd.getFieldTableName());
                filterDescriptors.add(desc);
            }
        }
	}
	
	/**
	 * 
	 * <b>Description:	将动态添加的表间关联条件去重合并</b><br>	
	 * 
	 */
    protected void mergeFilters() {
        // 添加动态的过滤描述符
        for (Entry<Class<?>, Map<String, List<DynamicFilterDescriptor>>> entry : addedFilters.entrySet()) {
            mergeFiltersByClz(entry.getKey());
            Map<String, List<DynamicFilterDescriptor>> map = entry.getValue();
            Iterator<String> fields = map.keySet().iterator();
            while (fields.hasNext()) {
                String fieldName = fields.next();
                FieldMetaData fmd = getFieldMetaByFieldName(entry.getKey(),
                        fieldName);
                List<DynamicFilterDescriptor> dfds = map.get(fieldName);
                for (DynamicFilterDescriptor dfd : dfds) {
                    FilterDescriptor desc = new FilterDescriptor(
                            getAliasByTableName(getTableNameByClass(entry.getKey()))
                                    + "." + getColumnName(fmd.getColumn()),
                            RabbitValueConverter.convert(dfd.getValue(), fmd, dfd.isReg()),
                            dfd.getFilter().value());
                    desc.setField(fmd.getField());
                    replaceKeyByReg(dfd, desc);
                    desc.setFilterTable(getTableNameByClass(entry.getKey()));
                    filterDescriptors.add(desc);
                }
            }
        }
    }

    /**
     * <b>Description  如果dfd中的key是正则表达式，则替换key</b>
     * @param dfd
     * @param desc
     */
    private void replaceKeyByReg(DynamicFilterDescriptor dfd,
            FilterDescriptor desc) {
        if (dfd.isReg()) {
        	String key = replaceRegByColumnName(dfd.getKeyReg(), desc.getField(), desc.getKey());
        	desc.setKey(key);
        }
    }

	/**
	 * <b>@description  用字段名替换正则中的内容 </b>
	 * @param reg		原始的正则表达式
	 * @param firstField		正则表达式中第一个字段的对应的field对象
	 * @param firstColumnName 	正则表达式中第一个字段的对应数据库字段
	 * @return
	 */
	protected String replaceRegByColumnName(String reg, Field firstField, String firstColumnName) {
		String key = reg.replaceFirst(REPLACE_WORD, firstColumnName);
		while (true) {
			String fieldName = getFieldByReg(key);
			if (fieldName.equals(key)) {
				break;
			}
			FieldMetaData fmd = getFieldMetaByFieldName(firstField.getDeclaringClass(), fieldName);
			String columnName = getColumnName(fmd.getColumn());
			key = key.replaceFirst(REPLACE_WORD, firstColumnName.split("\\.")[0] + "." + columnName);
		}
		return key;
	}
	
	public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
	
	protected String getTableNameByClass(Class<?> clz) {
	    //分片表在clzMapping中查出来的名字和getDeclaredTableName()一样
	    if (getEntityClz().equals(clz)) {
	        return metaData.getTableName();
	    } else {
	        return MetaData.getTableNameByClass(clz);
	    }
	}
	
	/**
	 * 
	 * <b>Description:	根据字段名和class信息获取字段对应的FieldMetaData信息</b><br>
	 * @param clz
	 * @param fieldName
	 * @return	
	 * 
	 */
    protected FieldMetaData getFieldMetaByFieldName(Class<?> clz, String fieldName) {
        return MetaData.getCachedFieldsMeta(clz, fieldName);
    }
	
	/**
	 * 
	 * <b>Description:	合并关联的filter</b><br>
	 * @param clz	
	 * 
	 */
    private void mergeFiltersByClz(Class<?> clz) {
        if (!dependencyPath.containsKey(clz)) {
            return;
        }
        for (int i = dependencyPath.get(clz).size() - 1; i >= 0; i--) {
            FilterDescriptor fd = dependencyPath.get(clz).get(i);
            boolean exist = false;
            for (FilterDescriptor fde : filterDescriptors) {
                if (fde.isEqual(fd)) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                filterDescriptors.add(fd);
            }
        }
    }
	
	protected abstract String getAliasByTableName(String tableName);
	
	/**
	 * 
	 * <b>Description:	核对查询路径， 同一个clz不允许添加多个查询路径</b><br>
	 * @param deps	
	 * 
	 */
    protected void checkQueryPath(Class<?>... deps) {
        List<FilterDescriptor> jds = findJoinDepFilterDescriptors(deps);
        if (!dependencyPath.containsKey(deps[0])) {
            dependencyPath.put(deps[0], jds);
            checkCyclePath(deps);
        } else {
            List<FilterDescriptor> exists = dependencyPath.get(deps[0]);
            if (exists.size() != jds.size()) {
                throw new InvalidQueryPathException(deps[0]);
            }
            for (int i = 0; i < jds.size(); i++) {
                if (exists.get(i).isEqual(jds.get(i))) {
                    continue;
                }
                throw new InvalidQueryPathException(deps[0]);
            }
        }
    }

    private void checkCyclePath(Class<?>... deps) {
        Map<Class<?>, Integer> counter = new HashMap<>();
        for (Class<?> clz : deps) {
            counter.put(clz, 0);
            for (Class<?> c : deps) {
                if (c.equals(clz)) {
                    counter.put(clz, counter.get(clz) + 1);
                }
            }
        }
        for (Entry<Class<?>, Integer> entry : counter.entrySet()) {
            if (entry.getValue() > 1) {
                throw new CycleDependencyException(entry.getKey());
            }
        }
    }

	/**
	 * 
	 * <b>Description:	获取JoinDependency FilterDescriptors</b><br>
	 * @param deps
	 * @return	
	 * 
	 */
    private List<FilterDescriptor> findJoinDepFilterDescriptors(
            Class<?>... deps) {
        List<FilterDescriptor> fds = getFilterDescriptorsByCache(deps);
        Map<Class<?>, List<FilterDescriptor>> clz2j = getClzesEnabled2Join();
        Class<?> last = deps[deps.length - 1];
        if (last.equals(getEntityClz())) {
            return fds;
        }
        while (true) {
            if (!clz2j.containsKey(last)) {
                throw new InvalidFetchOperationException(last
                        + " can't be joined by [" + getEntityClz().getName()
                        + "]");
            }
            List<FilterDescriptor> list = clz2j.get(last);
            if (list.size() != 1) {
                throw new AmbiguousDependencyException(last);
            }
            fds.add(list.get(0));
            if (list.get(0).getJoinDependency().equals(getEntityClz())) {
                break;
            }
            last = list.get(0).getJoinDependency();
        }
        return fds;
    }

    private List<FilterDescriptor> getFilterDescriptorsByCache(Class<?>... deps) {
        List<FilterDescriptor> fds = new ArrayList<>();
        Map<Class<?>, List<FilterDescriptor>> clz2j = getClzesEnabled2Join();
        for (int i = 0; i < deps.length; i++) {
            if (i == deps.length - 1) {
                break;
            }
            if (!clz2j.containsKey(deps[i])) {
                throw new RabbitDMLException(deps[i] + " can't be fetched by ["
                        + getEntityClz().getName() + "]");
            }
            List<FilterDescriptor> list = clz2j.get(deps[i]);
            boolean right = false;
            for (FilterDescriptor fd : list) {
                if (fd.getJoinDependency().equals(deps[i + 1])) {
                    right = true;
                    fds.add(fd);
                    break;
                }
            }
            if (!right) {
                throw new RabbitDMLException("wrong query dependency for "
                        + deps[i]);
            }
        }
        return fds;
    }
	
	/**
	 * 
	 * <b>Description:	获取能够被join查询的clz实体, 该对象不能全局缓存，因为别名可能不一样</b><br>
	 * @return	
	 * 
	 */
    public Map<Class<?>, List<FilterDescriptor>> getClzesEnabled2Join() {
        if (null == clzesEnabled2Join) {
            clzesEnabled2Join = new ConcurrentHashMap<>();
            findOutEnable2JoinClzes(getEntityClz());
        }
        return clzesEnabled2Join;
    }
	
	//找出该实体下所有能够被关联查询的类
	private void findOutEnable2JoinClzes(Class<?> clz) {
        for (FieldMetaData fmd : MetaData.getCachedFieldsMetas(clz).values()) {
            if (!fmd.isForeignKey()) {
                continue;
            }
            String fkName = getManyToOnePrimaryKeyColumnName(fmd);
            String fkTable = getTableNameByClass(fmd.getField().getType());
            String tableAlias = getAliasByTableName(fkTable);
			if (fmd.isMultiFetchField()) {
				tableAlias = tableAlias + UNDERLINE + fmd.getIndex();
			}
            FilterDescriptor desc = new FilterDescriptor(getAliasByTableName(getTableNameByClass(clz))
                            + "." + getColumnName(fmd.getColumn()), tableAlias + "." + fkName);
            desc.setField(fmd.getField());
            desc.setMultiFetchField(fmd.isMultiFetchField());
            desc.setIndex(fmd.getIndex());
            desc.setFilterTable(fkTable);
            desc.setJoinOn(true);
            desc.setJoinDependency(clz);
            desc.setJoinField(fmd.getField());
			//防止递归查询出现死循环
            if (null == clzesEnabled2Join.get(fmd.getField().getType())) {
                List<FilterDescriptor> list = new ArrayList<>();
                list.add(desc);
                clzesEnabled2Join.put(fmd.getField().getType(), list);
                recursiveFindOutEnable2JoinClzes(fmd);
            } else {
                if (!isDependencyExists(clz, fmd)) {
                    clzesEnabled2Join.get(fmd.getField().getType()).add(desc);
                    recursiveFindOutEnable2JoinClzes(fmd);
                }
            }
		}
	}
	
	
	/**
	 * <b>@description 在多对一条件下获取多对一的一端表关联字段 </b>
	 * @param fmd
	 * @return
	 */
	private String getManyToOnePrimaryKeyColumnName(FieldMetaData fmd) {
		Column column = fmd.getColumn();
		if ("".equals(column.joinFieldName().trim())) {
			return getColumnName(MetaData.getPrimaryKeyFieldMeta(fmd.getField().getType()).getColumn());
		} else {
			// 如果用户自定义了关联字段，就采用自定义的关联字段
			FieldMetaData cfm = MetaData.getCachedFieldsMeta(fmd.getField().getType(), column.joinFieldName().trim());
			return getSessionFactory().getColumnName(cfm.getColumn());
		}
	}

    private void recursiveFindOutEnable2JoinClzes(FieldMetaData fmd) {
        if (fmd.isMultiFetchField() && fmd.getIndex() != 1) {
            return;
        }
        findOutEnable2JoinClzes(fmd.getField().getType());
    }

    /**
     * 
     * <b>Description:  判断缓存中是否已经有依赖</b><br>.
     * @param clz       依赖class 
     * @param fmd
     * @return	
     * 
     */
    private boolean isDependencyExists(Class<?> clz, FieldMetaData fmd) {
        Class<?> type = fmd.getField().getType();
        for (FilterDescriptor fd : clzesEnabled2Join.get(type)) {
            if (fd.getJoinDependency().equals(clz)
                    && fd.getJoinField().equals(fmd.getField())) {
                return true;
            }
        }
        return false;
    }
	
	/**
	 * 
	 * 根据过滤器类型和值动态生成占位符
	 * @param filterType
	 * @param value
	 * @return
	 * 
	 */
    protected String createPlaceHolder(String filterType, Object value) {
        if (FilterType.IN.name().trim().equalsIgnoreCase(filterType.trim())) {
            StringBuilder sb = new StringBuilder("(");
            int size = 1;
            if (value.getClass().isArray()) {
                Object[] v = (Object[]) value;
                size = v.length;
            }
            if (value instanceof Collection) {
                size = ((Collection<?>) value).size();
            }
            for (int i = 0; i < size; i++) {
                sb.append("?,");
            }
            if (-1 != sb.lastIndexOf(",")) {
                sb.deleteCharAt(sb.lastIndexOf(","));
            }
            sb.append(")");
            return sb.toString();
        }
        return PLACE_HOLDER;
    }
	
	/**
	 * 
	 * <b>Description:	缓存jdbc存储过程中需要写入的值</b><br>
	 * @param value	
	 * @param field	
	 * 
	 */
    public void cachePreparedValues(Object value, Field field) {
        if (null == value) {
            preparedValues.add(new PreparedValue(null, field));
            return;
        }
        if (!byte[].class.equals(value.getClass()) && value.getClass().isArray()) {
            Object[] vs = (Object[]) value;
            for (Object v : vs) {
                preparedValues.add(new PreparedValue(v, field));
            }
            return;
        }
        if (value instanceof Collection) {
            Collection<?> c = (Collection<?>) value;
            for (Object v : c) {
                preparedValues.add(new PreparedValue(v, field));
            }
            return;
        }
        preparedValues.add(new PreparedValue(value, field));
    }
	
	/**
	 * 
	 * 从正则表达式中获取字段信息
	 * @param reg
	 * @return
	 * 
	 */
    public String getFieldByReg(String reg) {
        String field;
        Matcher matcher = pattern.matcher(reg);
        if (matcher.find()) {
            field = matcher.group(1);
        } else {
            field = reg;
        }
        return field;
    }
	
	/**
	 * 
	 * <b>Description:	核对正则表达式中的字段是否属于指定的class</b><br>
	 * @param target
	 * @param field	
	 * 
	 */
    public static Field checkField(Class<?> target, String field) {
        Map<String, FieldMetaData> cachedFieldsMetas = MetaData.getCachedFieldsMetas(target);
		FieldMetaData fieldMetaData = cachedFieldsMetas.get(field);
		if (null != fieldMetaData) {
			return fieldMetaData.getField();
		} else {
	        throw new UnKnownFieldException("field[" + field + "] does not belong to " + target);
		}
    }
	
	/**
	 * 
	 * <b>Description:	检查目标class是否可以被join过滤</b><br>
	 * @param target	
	 * 
	 */
    protected void checkJoinFilterClass(Class<?> target) {
        boolean validTarget = false;
        for (JoinFieldMetaData<?> jfm : metaData.getJoinMetas()) {
            if (jfm.getJoinClass().equals(target)) {
                validTarget = true;
                break;
            }
        }
        if (!validTarget) {
            throw new InvalidJoinFetchOperationException(target, getEntityClz());
        }
    }
	
	public MetaData<T> getMetaData() {
		return metaData;
	}
	
	/**
	 * 
	 * <b>Description:	生成内连接部分sql</b><br>
	 * @return	
	 * 
	 */
    protected StringBuilder generateInnerJoinSql() {
        StringBuilder sb = new StringBuilder();
        for (FilterDescriptor fd : filterDescriptors) {
            if (!fd.isJoinOn()) {
                continue;
            }
            sb.append(" INNER JOIN " + fd.getFilterTable() + " "
                    + getAliasByTableName(fd.getFilterTable()));
            sb.append(" ON " + fd.getKey() + fd.getFilter() + fd.getValue());
            sb.append(createInnerJoinFiltersSqlByDescriptor(fd));
        }
        return sb;
    }

    public String getColumnName(Column c) {
        return sessionFactory.getColumnName(c);
    }

	
    /**
     * <b>Description  根据FilterDescriptor生成内链接过滤条件sql片段</b>
     * @param fd
     * @return
     */
    private StringBuilder createInnerJoinFiltersSqlByDescriptor(
            FilterDescriptor fd) {
        StringBuilder sb = new StringBuilder();
        for (FilterDescriptor fdi : filterDescriptors) {
            if (fdi.isJoinOn()
                    || !fdi.getFilterTable().equals(fd.getFilterTable())) {
                continue;
            }
            String key = fdi.getKey();
            if (FilterType.IS.value().equals(fdi.getFilter().trim())
                    || FilterType.IS_NOT.value().equals(fdi.getFilter().trim())) {
                sb.append(" AND " + key + " " + fdi.getFilter() + NULL);
            } else {
                cachePreparedValues(fdi.getValue(), fdi.getField());
                sb.append(" AND " + key + fdi.getFilter()
                        + createPlaceHolder(fdi.getFilter(), fdi.getValue()));
            }
        }
        return sb;
    }

	/**
	 * 
	 * <b>Description:	创建过滤条件的sql</b><br>
	 * @return	
	 * 
	 */
	protected StringBuilder generateFilterSql() {
        StringBuilder fsql = new StringBuilder();
        if (filterDescriptors.isEmpty() || getFilterDescriptors().isEmpty()) {
            StringBuilder multiDropSql = createMultiDropSql();
            if (0 != multiDropSql.length()) {
                multiDropSql.insert(0, WHERE + " 1 = 1 ");
                fsql.append(multiDropSql);
            }
            return fsql;
        }
        fsql.append(createFilters());
        return fsql;
	}

	protected StringBuilder createMultiDropSql() {
		StringBuilder fsql = new StringBuilder();
		for (MultiDropFilter filter : multiDropFilters) {
        	fsql.append(" AND (");
            fsql.append(createMultiDropSql(filter));
            fsql.append(")");
        }
		return fsql;
	}
	
    private StringBuilder createFilters() {
        List<FilterDescriptor> fds = getFilterDescriptors();
        StringBuilder fsql = new StringBuilder();
        fsql.append(WHERE);
        for (int i = 0; i < fds.size(); i++) {
            FilterDescriptor fd = fds.get(i);
            String key = fd.getKey();
            String filter = fd.getFilter();
            if (FilterType.IS.value().equals(filter.trim())
                    || FilterType.IS_NOT.value().equals(filter.trim())) {
                fsql.append(key + " " + filter + NULL);
            } else {
                cachePreparedValues(fd.getValue(), fd.getField());
                fsql.append(key + filter
                        + createPlaceHolder(filter, fd.getValue()));
            }
            if (i != fds.size() - 1) {
                fsql.append(fd.getConnector());
            }
        }
        if (!hasMultiDropFilters()) {
        	return fsql;
        }
        for (MultiDropFilter filter : multiDropFilters) {
        	fsql.append(" AND (");
            fsql.append(createMultiDropSql(filter));
            fsql.append(")");
        }
        return fsql;
    }

	/**
	 * <b>@description 包含有效的or条件 </b>
	 * @return
	 */
	protected boolean hasMultiDropFilters() {
		if (multiDropFilters.isEmpty()) {
			return false;
		}
		for (MultiDropFilter filter : multiDropFilters) {
			if (!filter.getFilters().isEmpty()) {
				return true;
			}
		}
		return false;
	}

    /**
     * <b>@description 创建多分支（or条件）sql片段 </b>
     * @return
     */
    protected StringBuilder createMultiDropSql(MultiDropFilter multiDropFilter) {
        StringBuilder fsql = new StringBuilder();
        if (multiDropFilter.getFilters().isEmpty()) {
            return fsql;
        }
        int i = 0;
        for (MultiDropFilter filter : multiDropFilter.getFilters()) {
            FilterDescriptor fdi = filter.getFilterDescriptor();
            Column col = fdi.getField().getAnnotation(Column.class);
            String key = getAliasByTableName(getCurrentTableMeta().getTableName()) + "."
                    		+ getColumnName(col);
            Object value = RabbitValueConverter.convert(fdi.getValue(),
                            MetaData.getCachedFieldsMeta(getEntityClz(),
                            		filter.getKey()));
            if (FilterType.IS.value().equals(fdi.getFilter().trim())
                    || FilterType.IS_NOT.value().equals(fdi.getFilter().trim())) {
                fsql.append((i == 0 ? " " : " OR ") + key + " " + fdi.getFilter() + NULL);
            } else {
                cachePreparedValues(value, fdi.getField());
                fsql.append((i == 0 ? " " : " OR ") + key + " " + fdi.getFilter()
                        + createPlaceHolder(fdi.getFilter(), value));
            }
            i++;
        }
        return fsql;
    }

    private List<FilterDescriptor> getFilterDescriptors() {
        List<FilterDescriptor> fds = new ArrayList<>();
        for (FilterDescriptor fd : filterDescriptors) {
            if (fd.isJoinOn()) {
                continue;
            }
            if (fd.getFilterTable().equals(metaData.getTableName())) {
                fds.add(fd);
            }
        }
        return fds;
    }
	
    /**
     * <b>Description  获取主表的过滤条件描述符</b>
     * @return
     */
    protected List<FilterDescriptor> getMainFilterDescriptors() {
        List<FilterDescriptor> fds = new ArrayList<>();
        for (FilterDescriptor fd : filterDescriptors) {
            if (fd.getFilterTable().equals(metaData.getTableName())) {
                fds.add(fd);
            }
        }
        return fds;
    }

    /**
     * <b>Description  获取当前操作的分片表名</b>
     * @param factors
     * @return
     */
	protected TableMeta getCurrentShardedTableMeta(List<ShardFactor> factors) {
		List<TableMeta> hitTables = getShardingPolicy().getHitTables(getEntityClz(), getDeclaredTableName(), factors, getTableMetas());
		return hitTables.get(0);
	}
	
	/**
	 * <b>@description 获取所有分区表 </b>
	 * @return
	 */
	public List<TableMeta> getTableMetas() {
		return sessionFactory.getTableMetas(metaData.getShardingPolicy().getClass(), metaData.getEntityClz());
	}
    
    public TableMeta getCurrentTableMeta() {
        if (isShardingOperation()) {
            return getCurrentShardedTableMeta(getFactors());
        }
        return new TableMeta(metaData.getTableName(), sessionFactory.getDataSource());
    }

    /**
     * <b>Description  获取声明的原始表名</b>
     * @return
     */
    public String getDeclaredTableName() {
        return getEntityClz().getDeclaredAnnotation(Entity.class).value();
    }

    public ShardingPolicy getShardingPolicy() {
        return metaData.getShardingPolicy();
    }
    
    /**
     * <b>Description  判断该操作是否发生分片表上</b>
     * @return
     */
    protected boolean isShardingOperation() {
        return !DefaultShardingPolicy.class.equals(getShardingPolicy().getClass());
    }
    
	 /**
	 * 
	 * <b>Description:    关闭sql连接</b><br>.
	 * @param conn	
	 * 
	 */
    public static void closeConnection(Connection conn) {
        if (null == conn) {
            return;
        }
        try {
            conn.close();
        } catch (Exception e) {
        	// TO DO
        }
    }
    
    /**
     * 
     * <b>Description:   关闭jdbc存储过程 </b><br>.
     * @param stmt	
     * 
     */
    public static void closeStmt(PreparedStatement stmt) {
        if (null == stmt) {
            return;
        }
        try {
            stmt.close();
        } catch (Exception e) {
        	// TO DO
        }
    }

    protected void cacheMultiDropFilter(MultiDropFilter multiDropFilter) {
    	multiDropFilter.setTargetClz(metaData.getEntityClz());
        this.multiDropFilters.add(multiDropFilter);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void setValue2Field(Object target, Field field, Object value) {
        try {
        	field.setAccessible(true);
            if (Enum.class.isAssignableFrom(field.getType())) {
            	Class clz = field.getType();
                field.set(target, Enum.valueOf(clz, value.toString()));
            } else {
            	field.set(target, value);
            }
        } catch (Exception e) {
            throw new RabbitDMLException(e.getMessage(), e);
        }
    }

    public static <T> T newInstance(Class<T> clz) {
        try {
            return clz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RabbitDMLException(e);
        }
    }
    
    public boolean isForbiddenDynamic() {
		return forbiddenDynamic;
	}

    /**
     * 获取根据命名sql对象对应的目标表名，如果配置参数targetTableName则用配置参数
     * @param namedObject   命名对象
     */
	protected TableMeta getTableMetaByNamedObject(NamedSQL namedObject) {
        if (!SessionFactory.isEmpty(namedObject.getTargetTableName())) {
            return new TableMeta(namedObject.getTargetTableName().trim(), sessionFactory.getDataSource());
        }
        return new TableMeta(getMetaData().getTableName(), sessionFactory.getDataSource());
    }
	
	/**
	 * <b>Description      单个设值</b>
	 * @param fieldAlias   字段在sql中的别名
	 * @param value        字段的值
	 * @param fieldName    字段在对应实体中的名字  	 
	 * @param entityClz    字段所属的实体   			 
	 * @return
	 */
	protected void setVariable(String fieldAlias, Object value, String fieldName, Class<?> entityClz) {
	    List<Integer> indexes = namedObject.getFieldIndexes(fieldAlias);
	    for (int index : indexes) {
	        if (null != entityClz && !SessionFactory.isEmpty(fieldName)) {
	            try {
	                fieldsValues.put(index, new PreparedValue(value, entityClz.getDeclaredField(fieldName)));
	            } catch (Exception e) {
	                throw new UnKnownFieldException(e.getMessage());
	            }
	        } else {
	            fieldsValues.put(index, new PreparedValue(value));
	        }
	    }
	}
	
	protected DialectTransformer getTransformer() {
		return DialectTransformer.getTransformer(sessionFactory.getDialectType());
	}
	
	public List<CallBackTask> getFilterTasks() {
		return filterTasks;
	}
	
}

package rabbit.open.orm.dml;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.FilterType;
import rabbit.open.orm.dml.filter.DMLType;
import rabbit.open.orm.dml.filter.PreparedValue;
import rabbit.open.orm.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.dml.meta.FieldMetaData;
import rabbit.open.orm.dml.meta.FilterDescriptor;
import rabbit.open.orm.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.dml.meta.JoinFilter;
import rabbit.open.orm.dml.meta.MetaData;
import rabbit.open.orm.dml.util.SQLFormater;
import rabbit.open.orm.exception.AmbiguousDependencyException;
import rabbit.open.orm.exception.CycleDependencyException;
import rabbit.open.orm.exception.InvalidFetchOperationException;
import rabbit.open.orm.exception.InvalidJoinFetchOperationException;
import rabbit.open.orm.exception.InvalidQueryPathException;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.exception.UnKnownFieldException;
import rabbit.open.orm.pool.SessionFactory;
import rabbit.open.orm.shard.ShardFactor;
import rabbit.open.orm.shard.ShardingPolicy;

/**
 * <b>Description: 	所有dml操作的基类</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public abstract class DMLAdapter<T> {

    //换行
	private static final String NEW_LINE = "\n";

    protected static final String UNDERLINE = "_";

    Logger logger = Logger.getLogger(getClass());
	
	protected List<CallBackTask> filterTasks;
	
	protected SessionFactory sessionFactory;
	
	protected static final String SEPARATOR = "$";
	
	protected List<Object> preparedValues = new ArrayList<>();
	
	protected static final String PLACE_HOLDER = "?";
	
	//字段替换符号"${}"的正则表达式
	protected static final String REPLACE_WORD = "\\$\\{(.*?)\\}";
	
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
	
	//动态添加的过滤器
	protected Map<Class<?>, Map<String, List<DynamicFilterDescriptor>>> addedFilters;

	//动态添加的left join过滤器
	protected Map<Class<?>, Map<String, List<DynamicFilterDescriptor>>> addedJoinFilters;

	//动态添加的inner join过滤器
	protected Map<Class<?>, JoinFilter> joinFilters = new HashMap<>();
	
	//缓存class的依赖路径, 同一个clz不能有多个路径
	protected Map<Class<?>, List<FilterDescriptor>> dependencyPath = new HashMap<>();

	public DMLAdapter(SessionFactory sessionFactory, Class<T> clz) {
		this(sessionFactory, null, clz);
	}

	@SuppressWarnings("unchecked")
    public DMLAdapter(SessionFactory sessionFactory, T filterData, Class<T> clz) {
		this.sessionFactory = sessionFactory;
		this.filterData = filterData;
		metaData = (MetaData<T>) MetaData.getMetaByClass(clz);
		pattern  = Pattern.compile(REPLACE_WORD);
		addedFilters = new HashMap<>();
		addedJoinFilters = new HashMap<>();
		filterTasks = new ArrayList<>();
	}
	
	/**
	 * 
	 * <b>Description:	打印sql</b><br>	
	 * 
	 */
	public void showSql(){
	    if(null == sql || 0 == sql.length() || !sessionFactory.isShowSql()){
            return;
        }
		if(sessionFactory.isMaskPreparedSql()){
		    showMaskedPreparedSql();
		}else{
		    showUnMaskedSql();
		}
	}

    /**
     * <b>Description  显示带问号的sql.</b>
     */
    private void showMaskedPreparedSql() {
        logger.info(NEW_LINE + (sessionFactory.isFormatSql() ? 
                SQLFormater.format(sql.toString()) : sql.toString()));
        
    }

    /**
     * <b>Description  显示带真实值的sql.</b>
     */
    private void showUnMaskedSql() {
        try{
			String valuesql = sql.toString();
			StringBuilder vs = new StringBuilder("prepareStatement values(");
			for(Object v : preparedValues){
			    Object text = convert2Str((PreparedValue) v);
				valuesql = replace(valuesql, text.toString());
				vs.append(text + ", ");
			}
			if(-1 != vs.indexOf(",")){
				int index = vs.lastIndexOf(",");
				vs.deleteCharAt(index);
				vs.deleteCharAt(index);
			}
			vs.append(")");
			logger.info(NEW_LINE + (sessionFactory.isFormatSql() ? SQLFormater.format(valuesql) : valuesql)
					+ (preparedValues.isEmpty() ? "" : (NEW_LINE + vs.toString())));
		}catch(Exception e){
			logger.error("show sql error for " + e.getMessage(), e);
		}
    }

    /**
     * 
     * <b>Description:    转成字符串</b><br>.
     * @param v
     * @return	
     * 
     */
    private Object convert2Str(PreparedValue v) {
        if(null == v || null == v.getValue()){
        	return "null";
        }
    	if(v.getValue() instanceof String){
    		return "'" + v.getValue().toString() + "'";
    	}
    	if(v.getValue() instanceof Date){
    		String ds = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(v.getValue());
    		if(sessionFactory.getDialectType().isOracle()){
    			return "to_date('" + ds + "','YYYY-MM-DD HH24:MI:SS')";
    		}else{
    			return "'" + ds + "'";
    		}
        }
        return v.getValue();
    }
	
	private String replace(String src, String replace){
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
	protected void setPreparedStatementValue(PreparedStatement stmt, DMLType dmlType) throws SQLException{
        for (int i = 1; i <= preparedValues.size(); i++) {
            PreparedValue pv = (PreparedValue) preparedValues.get(i - 1);
            Object value = sessionFactory.onValueSetted(pv, dmlType);
            if (value instanceof Date) {
                stmt.setTimestamp(i, new Timestamp(((Date) value).getTime()));
            } else if (value instanceof Double){ 
                stmt.setDouble(i, (double) value);
            } else if (value instanceof Float){ 
                stmt.setFloat(i, (float) value);
            } else {
                stmt.setObject(i, value);
            }
        }
	}
	
	/**
	 * 
	 * 准备过滤的filterDescriptors信息
	 */
	protected void prepareFilterMetas(){
		if(this.filterData != null){
			generateFilters(getNonEmptyFieldMetas(this.filterData, metaData.getEntityClz()));
		}
	}
	
	/**
	 * 
	 * <b>Description:	执行动态回调任务</b><br>	
	 * 
	 */
	protected void runCallBackTask(){
		if(filterTasks.isEmpty()){
			return;
		}
		for(CallBackTask dft : filterTasks){
			dft.run();
		}
	}
	
	/**
	 * 
	 * 获取条件对象中有值的字段映射信息
	 * @param 	data
	 * @param 	entityClz
	 * 
	 */
	protected final List<FieldMetaData> getNonEmptyFieldMetas(Object data, Class<?> clz) {
		if(null == data){
			return new ArrayList<>();
		}
		String tableName = getTableNameByClass(clz);
		List<FieldMetaData> fields = new ArrayList<>();
		Class<?> clzSuper = clz;
		while(!clzSuper.equals(Object.class)){
			for(Field f : clzSuper.getDeclaredFields()){
				Column col = f.getAnnotation(Column.class);
				f.setAccessible(true);
				Object fieldValue = getValue(f, data);
				if(null == col || null == fieldValue){
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
     * @param pk
     * @param target
     * @return  
     * 
     */
    protected Object getValue(Field pk, Object target){
        try {
            return pk.get(target);
        } catch (Exception e) {
            throw new RabbitDMLException(e.getMessage());
        }
    }
	
	//生成过滤条件
	protected void generateFilters(List<FieldMetaData> fieldMetas) {
		for(FieldMetaData fmd : fieldMetas){
			if(fmd.isForeignKey()){
				String fkName = MetaData.getPrimaryKeyField(fmd.getFieldValue().getClass()).getAnnotation(Column.class).value();
				String fkTable = getTableNameByClass(fmd.getField().getType());
				MetaData.updateTableMapping(fkTable, fmd.getField().getType());
				FilterDescriptor desc = new FilterDescriptor(getAliasByTableName(fmd.getFieldTableName()) + "." + fmd.getColumn().value(), 
						getAliasByTableName(fkTable) + "." + fkName);
				desc.setField(fmd.getField());
				desc.setJoinOn(true);
				desc.setFilterTable(fkTable);
				filterDescriptors.add(desc);
				generateFilters(getNonEmptyFieldMetas(fmd.getFieldValue(), fmd.getField().getType()));
			}else{
				FilterDescriptor desc = new FilterDescriptor(getAliasByTableName(fmd.getFieldTableName())+ "." + fmd.getColumn().value(), 
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
	protected void combineFilters(){
		//添加动态的过滤描述符
		for(Entry<Class<?>, Map<String, List<DynamicFilterDescriptor>>> entry : addedFilters.entrySet()){
			combineFiltersByClz(entry.getKey());
			Map<String, List<DynamicFilterDescriptor>> map = entry.getValue();
			Iterator<String> fields = map.keySet().iterator();
			while(fields.hasNext()){
				String fieldName = fields.next();
				FieldMetaData fmd = getFieldMetaByFieldName(entry.getKey(), fieldName);
				List<DynamicFilterDescriptor> dfds = map.get(fieldName);
				for(DynamicFilterDescriptor dfd : dfds){
					FilterDescriptor desc = new FilterDescriptor(
							getAliasByTableName(getTableNameByClass(entry.getKey())) + "." 
							        + fmd.getColumn().value(), 
							RabbitValueConverter.convert(dfd.getValue(), fmd), 
							dfd.getFilter().value());
					desc.setField(fmd.getField());
					if(dfd.isReg()){
						desc.setKey(dfd.getKeyReg().replaceAll(REPLACE_WORD, desc.getKey()));
					}
					desc.setFilterTable(getTableNameByClass(entry.getKey()));
					filterDescriptors.add(desc);
				}
			}
		}
	}
	
	protected String getTableNameByClass(Class<?> clz) {
	    if (metaData.getEntityClz().equals(clz)) {
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
	private FieldMetaData getFieldMetaByFieldName(Class<?> clz, String fieldName){
		List<FieldMetaData> cachedFieldsMetas = MetaData.getCachedFieldsMetas(clz);
		for(FieldMetaData fmd : cachedFieldsMetas){
			if(fmd.getField().getName().equals(fieldName)){
				return fmd;
			}
		}
		throw new RabbitDMLException("no field meta info is found for[" + fieldName + "]");
	}
	
	/**
	 * 
	 * <b>Description:	合并关联的filter</b><br>
	 * @param clz	
	 * 
	 */
	private void combineFiltersByClz(Class<?> clz) {
		if(!dependencyPath.containsKey(clz)){
			return;
		}
		for(int i = dependencyPath.get(clz).size() - 1; i >= 0; i--){
			FilterDescriptor fd = dependencyPath.get(clz).get(i);
			boolean exist = false;
			for(FilterDescriptor fde : filterDescriptors){
				if(fde.isEqual(fd)){
					exist = true;
					break;
				}
			}
			if(!exist){
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
		if(!dependencyPath.containsKey(deps[0])){
			dependencyPath.put(deps[0], jds);
			checkCyclePath(deps);
		}else{
			List<FilterDescriptor> exists = dependencyPath.get(deps[0]);
			if(exists.size() != jds.size()){
				throw new InvalidQueryPathException(deps[0]);
			}
			for(int i = 0; i < jds.size(); i++){
				if(exists.get(i).isEqual(jds.get(i))){
					continue;
				}
				throw new InvalidQueryPathException(deps[0]);
			}
		}
	}

    private void checkCyclePath(Class<?>... deps) {
        Map<Class<?>, Integer> counter = new HashMap<>();
        for(Class<?> clz : deps){
            counter.put(clz, 0);
            for(Class<?> c : deps){
                if(c.equals(clz)){
                    counter.put(clz, counter.get(clz) + 1);
                }
            }
        }
        for(Entry<Class<?>, Integer> entry : counter.entrySet()){
            if(entry.getValue() > 1){
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
	private List<FilterDescriptor> findJoinDepFilterDescriptors(Class<?>... deps) {
	    List<FilterDescriptor> fds = getFilterDescriptorsByCache(deps);
		Map<Class<?>, List<FilterDescriptor>> clz2j = getClzesEnabled2Join();
		Class<?> last = deps[deps.length - 1];
		if(last.equals(metaData.getEntityClz())){
			return fds;
		}
		while(true){
			if(!clz2j.containsKey(last)){
				throw new InvalidFetchOperationException(last + " can't be joined by [" + metaData.getEntityClz().getName() + "]");
			}
			List<FilterDescriptor> list = clz2j.get(last);
			if(list.size() != 1){
				throw new AmbiguousDependencyException(last);
			}
			fds.add(list.get(0));
			if(list.get(0).getJoinDependency().equals(metaData.getEntityClz())){
				break;
			}
			last = list.get(0).getJoinDependency();
		}
		return fds;
	}

    private List<FilterDescriptor> getFilterDescriptorsByCache(Class<?>... deps) {
        List<FilterDescriptor> fds = new ArrayList<>();
        Map<Class<?>, List<FilterDescriptor>> clz2j = getClzesEnabled2Join();
		for(int i = 0; i < deps.length; i++){
			if(i == deps.length - 1){
				break;
			}
			if(!clz2j.containsKey(deps[i])){
				throw new RabbitDMLException(deps[i] + " can't be fetched by [" + metaData.getEntityClz().getName() + "]");
			}
			List<FilterDescriptor> list = clz2j.get(deps[i]);
			boolean right = false;
			for(FilterDescriptor fd : list){
				if(fd.getJoinDependency().equals(deps[i + 1])){
					right = true;
					fds.add(fd);
					break;
				}
			}
			if(!right){
				throw new RabbitDMLException("wrong query dependency for " + deps[i]);
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
	protected Map<Class<?>, List<FilterDescriptor>> getClzesEnabled2Join(){
		if(null == clzesEnabled2Join){
			clzesEnabled2Join = new ConcurrentHashMap<>();
			findOutEnable2JoinClzes(metaData.getEntityClz());
		}
		return clzesEnabled2Join;
	}
	
	//找出该实体下所有能够被关联查询的类
	private void findOutEnable2JoinClzes(Class<?> clz){
        for (FieldMetaData fmd : MetaData.getCachedFieldsMetas(clz)) {
            if (!fmd.isForeignKey()) {
                continue;
            }
            String fkName = MetaData.getPrimaryKeyField(fmd.getField().getType())
                    .getAnnotation(Column.class).value();
            String fkTable = getTableNameByClass(fmd.getField().getType());
            String tableAlias = getAliasByTableName(fkTable);
            if(fmd.isMutiFetchField()){
                tableAlias = tableAlias + UNDERLINE + fmd.getIndex();
            }
            FilterDescriptor desc = new FilterDescriptor(getAliasByTableName(getTableNameByClass(clz))
                            + "." + fmd.getColumn().value(), tableAlias + "." + fkName);
            desc.setField(fmd.getField());
            desc.setMultiFetchField(fmd.isMutiFetchField());
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
                recusiveFindOutEnable2JoinClzes(fmd);
            } else {
                if (!isDependencyExists(clz, fmd)) {
                    clzesEnabled2Join.get(fmd.getField().getType()).add(desc);
                    recusiveFindOutEnable2JoinClzes(fmd);
                }
            }
		}
	}

    private void recusiveFindOutEnable2JoinClzes(FieldMetaData fmd) {
        if(fmd.isMutiFetchField() && fmd.getIndex() != 1){
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
        for(FilterDescriptor fd : clzesEnabled2Join.get(type)){
        	if(fd.getJoinDependency().equals(clz) && fd.getJoinField().equals(fmd.getField())){
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
	protected String createPlaceHolder(String filterType, Object value){
		if(FilterType.IN.name().trim().equalsIgnoreCase(filterType.trim())){
			StringBuilder sb = new StringBuilder("(");
			int size = 1;
			if(value.getClass().isArray()){
				Object[] v = (Object[]) value;
				size = v.length;
			}
			if(value instanceof Collection){
				size = ((Collection<?>)value).size();
			}
			for(int i = 0; i < size; i++){
				sb.append("?,");
			}
			if(-1 != sb.lastIndexOf(",")){
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
	protected void cachePreparedValues(Object value, Field field){
		if(null == value){
			preparedValues.add(new PreparedValue(null, field));
			return;
		}
		if(value.getClass().isArray()){
		    Object[] vs = (Object[]) value;
			for(Object v : vs){
				preparedValues.add(new PreparedValue(v, field));
			}
			return;
		}
		if(value instanceof Collection){
            Collection<?> c = (Collection<?>)value;
			for(Object v : c){
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
	protected String getFieldByReg(String reg) {
		String field;
		Matcher matcher = pattern.matcher(reg);
		if(matcher.find()){
			field = matcher.group(1);
		}else{
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
	protected void checkField(Class<?> target, String field) {
		boolean isValidField = false;
		List<FieldMetaData> cachedFieldsMetas = MetaData.getCachedFieldsMetas(target);
		for(FieldMetaData fmd : cachedFieldsMetas){
		    if(fmd.getField().getName().equals(field)){
		        isValidField = true;
		        break;
		    }
		}
		if(!isValidField){
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
		for(JoinFieldMetaData<?> jfm : metaData.getJoinMetas()){
			if(jfm.getJoinClass().equals(target)){
				validTarget = true;
				break;
			}
		}
		if(!validTarget){
			throw new InvalidJoinFetchOperationException(target, metaData.getEntityClz());
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
	protected StringBuilder generateInnerJoinsql() {
		StringBuilder sb = new StringBuilder();
		for(FilterDescriptor fd : filterDescriptors){
			if(!fd.isJoinOn()){
				continue;
			}
			sb.append(" INNER JOIN " + fd.getFilterTable() + " " + getAliasByTableName(fd.getFilterTable()));
			sb.append(" ON " + fd.getKey() + fd.getFilter() + fd.getValue());
			for(FilterDescriptor fdi : filterDescriptors){
				if(fdi.isJoinOn() || !fdi.getFilterTable().equals(fd.getFilterTable())){
					continue;
				}
				String key = fdi.getKey();
				if(FilterType.IS.value().equals(fdi.getFilter().trim()) 
						|| FilterType.IS_NOT.value().equals(fdi.getFilter().trim())){
					sb.append(" AND " + key + " " + fdi.getFilter() + " NULL ");
				}else{
					cachePreparedValues(fdi.getValue(), fdi.getField());
					sb.append(" AND " + key + fdi.getFilter() + createPlaceHolder(fdi.getFilter(), fdi.getValue()));
				}
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
		StringBuilder filterSql = new StringBuilder();
		if(filterDescriptors.isEmpty()){
			return filterSql;
		}
		List<FilterDescriptor> fds = getFilterDescriptors();
		if(fds.isEmpty()){
		    return filterSql;
		}
		appendFilters(filterSql, fds);
		return filterSql;
	}

    private void appendFilters(StringBuilder filterSql,
            List<FilterDescriptor> fds) {
        filterSql.append(" WHERE ");
        for(int i = 0; i < fds.size(); i++){
            FilterDescriptor fd = fds.get(i);
            String key = fd.getKey();
            String filter = fd.getFilter();
            if(FilterType.IS.value().equals(filter.trim()) 
                    || FilterType.IS_NOT.value().equals(filter.trim())){
                filterSql.append(key + " " + filter + " NULL ");
            }else{
                cachePreparedValues(fd.getValue(), fd.getField());
                filterSql.append(key + filter + createPlaceHolder(filter, fd.getValue()));
            }
            if(i != fds.size() - 1){
                filterSql.append(fd.getConnector());
            }
        }
    }

    private List<FilterDescriptor> getFilterDescriptors() {
        List<FilterDescriptor> fds = new ArrayList<>();
        for(FilterDescriptor fd : filterDescriptors){
			if(fd.isJoinOn()){
				continue;
			}
			if(fd.getFilterTable().equals(metaData.getTableName())){
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
        for(FilterDescriptor fd : filterDescriptors){
            if(fd.getFilterTable().equals(metaData.getTableName())){
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
    protected String getCurrentShardedTableName(List<ShardFactor> factors){
        String tableName = getDeclaredTableName();
        return getShardingPolicy().getShardingTable(metaData.getEntityClz(), tableName, factors);
    }

    /**
     * <b>Description  获取声明的原始表名</b>
     * @return
     */
    protected String getDeclaredTableName() {
        return metaData.getEntityClz().getDeclaredAnnotation(Entity.class).value();
    }

    protected ShardingPolicy getShardingPolicy() {
        return metaData.getShardingPolicy();
    }
    
    /**
     * <b>Description  判断该操作是否发生分片表上</b>
     * @return
     */
    protected boolean isShardingOperation() {
        return !ShardingPolicy.class.equals(getShardingPolicy().getClass());
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
        } catch (SQLException e) {
            throw new RabbitDMLException(e.getMessage(), e);
        }
    }
    
    /**
     * 
     * <b>Description:   关闭jdbc存储过程 </b><br>.
     * @param stmt	
     * 
     */
    public static void closeStmt(PreparedStatement stmt) {
        if(null == stmt){
            return;
        }
        try {
            stmt.close();
        } catch (SQLException e) {
            throw new RabbitDMLException(e);
        }
    }

}

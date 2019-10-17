package rabbit.open.orm.core.dml;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.StringUtils;

import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.annotation.ManyToMany;
import rabbit.open.orm.common.annotation.OneToMany;
import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.AmbiguousDependencyException;
import rabbit.open.orm.common.exception.ConflictFilterException;
import rabbit.open.orm.common.exception.CycleFetchException;
import rabbit.open.orm.common.exception.FetchShardEntityException;
import rabbit.open.orm.common.exception.InvalidFetchOperationException;
import rabbit.open.orm.common.exception.InvalidJoinFetchOperationException;
import rabbit.open.orm.common.exception.OrderAssociationException;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.common.exception.RepeatedAliasException;
import rabbit.open.orm.common.exception.RepeatedDMLFilterException;
import rabbit.open.orm.common.exception.RepeatedFetchOperationException;
import rabbit.open.orm.common.shard.ShardFactor;
import rabbit.open.orm.common.shard.ShardingPolicy;
import rabbit.open.orm.core.dml.filter.DMLFilter;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.FilterDescriptor;
import rabbit.open.orm.core.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.MultiDropFilter;
import rabbit.open.orm.datasource.Session;

/**
 * <b>Description: 	查询操作</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 *
 */
public abstract class AbstractQuery<T> extends DMLObject<T> {

	//标记当前查询对象是否已经执行过了
	private boolean run = false;

	private static final String AND = " AND ";

	private static final String INNER_JOIN = " INNER JOIN ";

	private static final String LEFT_JOIN = " LEFT JOIN ";

	// 希望查询出来的实体
	protected HashSet<Class<?>> entity2Fetch = new HashSet<>();

	// manyToOne的过滤条件
	protected List<FilterDescriptor> many2oneFilterDescriptors = new ArrayList<>();

	// 中间表过滤字段的值map
	private Map<Class<?>, Object> joinColumnFilterValueMap = new HashMap<>();

	// 是否分页查询
	protected boolean page = false;

	protected int pageSize = 0;

	protected int pageIndex = 0;

	protected boolean distinct = false;

	protected Map<Class<?>, HashSet<String>> asc = new ConcurrentHashMap<>();

	protected Map<Class<?>, HashSet<String>> desc = new ConcurrentHashMap<>();

	// 缓存需要fetch的对象
	private Map<Class<?>, Class<?>> fetchClasses = new ConcurrentHashMap<>();

	//别名映射key是表名
	protected Map<String, String> aliasMapping = new HashMap<>();

	//一对多查询条件
	protected List<JoinFieldMetaData<?>> joinFieldMetas = new ArrayList<>();

	//映射clz的fetch次数
	private Map<Class<?>, Integer> fetchTimesMappingTable = new HashMap<>();

	//缓存当前查询对象关心的字段名
	private Map<Class<?>, HashSet<String>> concernFields = new HashMap<>();

	//默认允许查询时触发DMLFilter
	private boolean enableGetFilter = true;

	public AbstractQuery() {}
	
	public AbstractQuery(SessionFactory factory, Class<T> clz) {
		super(factory, clz);
	}

	public AbstractQuery(SessionFactory factory, T filterData, Class<T> clz) {
		super(factory, filterData, clz);
	}

	// 缓存查询结果集的column header信息
	private List<String[]> colHeaders = new ArrayList<>();

	/**
	 *
	 * <b>Description:	执行sql命令</b><br>
	 * @return
	 *
	 */
	public Result<T> execute() {
		createQuerySql();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = sessionFactory.getConnection(getEntityClz(), getCurrentTableName(), DMLType.SELECT);
			stmt = conn.prepareStatement(sql.toString());
			setPreparedStatementValue(stmt, DMLType.SELECT);
			showSql();
			rs = stmt.executeQuery();
			List<T> resultList = readDataFromResultSets(rs);
			rs.close();
			return new Result<>(resultList);
		} catch (Exception e) {
			showUnMaskedSql(false);
			Session.flagException();
			throw new RabbitDMLException(e.getMessage(), e);
		} finally {
			closeResultSet(rs);
			DMLObject.closeStmt(stmt);
			closeConnection(conn);
			Session.clearException();
			setRun();
		}
	}

	private void setRun() {
		run = true;
	}

	private AbstractQuery<T> setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
		page = true;
		return this;
	}

	private boolean isEnableGetFilter() {
		return enableGetFilter;
	}

	public AbstractQuery<T> enableGetFilter() {
		enableGetFilter = true;
		return this;
	}

	public AbstractQuery<T> disableGetFilter() {
		enableGetFilter = false;
		return this;
	}

	/**
	 * <b>Description 设置只想查询出来的字段（主键字段会默认带出） </b>
	 * @param fields
	 * @author 肖乾斌
	 */
	public final AbstractQuery<T> queryFields(Class<?> clz, String... fields) {
		for (String field : fields) {
			DMLObject.checkField(clz, field);
		}
		if (!concernFields.containsKey(clz)) {
			concernFields.put(clz, new HashSet<>());
			concernFields.get(clz).add(MetaData.getPrimaryKeyField(clz).getName());
		}
		for (String field : fields) {
			concernFields.get(clz).add(field);
		}
		return this;
	}

	/**
	 * <b>Description 设置只想查询出来的字段（主键字段会默认带出） </b>
	 * @param field
	 * @return
	 */
	public final AbstractQuery<T> queryFields(String... field) {
		return queryFields(getMetaData().getEntityClz(), field);
	}

	/**
	 * <b>@description只查询指定的id，主键字段不会被自动带出</b>
	 * @param fields
	 * @return
	 */
	public final AbstractQuery<T> querySpecifiedFields(String... fields) {
		return querySpecifiedFields(getMetaData().getEntityClz(), fields);
	}

	/**
	 * 只查询指定的id，主键字段不会被自动带出
	 * @param clz
	 * @param fields
	 * @return
	 */
	public final AbstractQuery<T> querySpecifiedFields(Class<?> clz, String... fields) {
		for (String field : fields) {
			DMLObject.checkField(clz, field);
		}
		if (!concernFields.containsKey(clz)) {
			concernFields.put(clz, new HashSet<>());
		}
		for (String field : fields) {
			concernFields.get(clz).add(field);
		}
		return this;
	}

	/**
	 * <b>Description 标记只查询实体concern的字段 </b>
	 * @param clzs
	 * @return
	 * @author 肖乾斌
	 */
	public final AbstractQuery<T> tagConcern(Class<?>... clzs) {
		for (Class<?> clz : clzs) {
			Entity entity = clz.getAnnotation(Entity.class);
			if (null == entity || 0 == entity.concern().length) {
				continue;
			}
			queryFields(entity.concern());
		}
		return this;
	}

	public final AbstractQuery<T> tagConcern() {
		return tagConcern(getMetaData().getEntityClz());
	}

	/**
	 *
	 * <b>Description:	分页</b><br>
	 * @param pageIndex
	 * @param pageSize
	 * @return
	 *
	 */
	public AbstractQuery<T> page(int pageIndex, int pageSize) {
		setPageIndex(pageIndex);
		setPageSize(pageSize);
		return this;
	}

	private AbstractQuery<T> setPageSize(int pageSize) {
		this.pageSize = pageSize;
		page = true;
		return this;
	}

	private List<T> readDataFromResultSets(ResultSet rs) throws SQLException {
		List<T> resultList = new ArrayList<>();
		cacheResultColumns(rs);
		while (rs.next()) {
			T rowData = readRowData(rs);
			boolean exist = false;
			for (int i = 0; i < resultList.size(); i++) {
				T existedRowData = resultList.get(i);
				if (isSameRow(rowData, existedRowData)) {
					exist = true;
					combineRow(existedRowData, rowData);
					break;
				}
			}
			if (!exist) {
				resultList.add(rowData);
			}
		}
		return resultList;
	}

	/**
	 * 缓存查询结果集的头信息
	 * @param rs		split操作属于耗时操作，大量结果集查询时，该操作耗时较多
	 * @throws SQLException
	 */
	private void cacheResultColumns(ResultSet rs) throws SQLException {
		colHeaders.clear();
		for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
			colHeaders.add(rs.getMetaData().getColumnLabel(i).split("\\" + SEPARATOR));
		}
	}

	/**
	 * 判断两个bean是相同的一条记录
	 * @param rowData
	 * @param existedRowData
	 * @return
	 */
	private boolean isSameRow(T rowData, T existedRowData) {
		if (null != getPrimaryKeyValue(rowData)) {
			return getPrimaryKeyValue(rowData).equals(getPrimaryKeyValue(existedRowData));
		} else {
			return false;
		}
	}

	/**
	 *
	 * 合并结果集，将新读取的rowData合并到target中，合并list对象，递归合并Entity
	 * @param 	target
	 * @param 	rowData
	 *
	 */
	@SuppressWarnings({"unchecked" })
	private void combineRow(T target, T rowData) {
		for (JoinFieldMetaData<?> jfm : joinFieldMetas) {
			Object realTarget = getRealTarget(target, jfm);
			Object realRowData = getRealTarget(rowData, jfm);
			Field field = jfm.getField();
			if (null == realRowData || null == getValue(field, realRowData)) {
				continue;
			}
			List<Object> list = (List<Object>) getValue(field, realTarget);
			if (null == list) {
				setValue2Field(realTarget, jfm.getField(),
						getValue(field, realRowData));
			} else {
				combineList(realRowData, field, list);
			}
		}
	}

	private Object getRealTarget(T target, JoinFieldMetaData<?> jfm) {
		Object realTarget = target;
		if (!jfm.getTargetClass().equals(target.getClass())) {
			for (Field f : jfm.getDependencyFields()) {
				realTarget = getValue(f, realTarget);
				if (null == realTarget) {
					return null;
				}
			}
		}
		return realTarget;
	}

	@SuppressWarnings("rawtypes")
	private void combineList(Object rowData, Field field, List<Object> list) {
		List<?> listNew = (List) getValue(field, rowData);
		if (null != listNew) {
			for (Object o : listNew) {
				if (!contains(list, o)) {
					list.add(o);
				}
			}
		}
	}

	private boolean contains(List<?> list, Object o) {
		for (Object e : list) {
			if (getPrimaryKeyValue(o).equals(getPrimaryKeyValue(e))) {
				return true;
			}
		}
		return false;
	}

	private Object getPrimaryKeyValue(Object readRowData) {
		Field primaryKey = MetaData.getPrimaryKeyField(readRowData.getClass());
		return getValue(primaryKey, readRowData);
	}

	/**
	 *
	 * 读取当前行的数据
	 * @param  rs
	 * @throws SQLException
	 *
	 */
	@SuppressWarnings("unchecked")
	private T readRowData(ResultSet rs) throws SQLException {
		// 缓存【表别名】和实体对象
		Map<String, Object> fetchEntity = new HashMap<>();
		// 缓存缓存【表名】和joinFetch的实体
		Map<String, Object> joinFetchEntity = new HashMap<>();
		readEntity(rs, fetchEntity, joinFetchEntity);
		if (null == fetchEntity.get(getAliasByTableName(metaData.getTableName()))) {
			fetchEntity.put(getAliasByTableName(metaData.getTableName()),
					DMLObject.newInstance(metaData.getEntityClz()));
		}
		T target = (T) fetchEntity.get(getAliasByTableName(metaData.getTableName()));
		for (Object entity : fetchEntity.values()) {
			if (entity == target) {
				continue;
			}
			injectFetchDependency(fetchEntity, entity);
		}
		for (Object entity : joinFetchEntity.values()) {
			if (entity == target) {
				continue;
			}
			injectJoinDependency(fetchEntity, entity);
		}
		return target;
	}

	private void injectJoinDependency(Map<String, Object> fetchEntity, Object entity) {
		for (JoinFieldMetaData<?> jfd : joinFieldMetas) {
			if (!entity.getClass().equals(jfd.getJoinClass())) {
				continue;
			}
			ArrayList<Object> list = new ArrayList<>();
			list.add(entity);
			Object target = fetchEntity.get(getAliasByTableName(getTableNameByClass(jfd
					.getTargetClass())));
			setValue2Field(target, jfd.getField(), list);
		}
	}

	/**
	 * 注入fetch对象
	 * @param fetchEntity
	 * @param entity
	 */
	private void injectFetchDependency(Map<String, Object> fetchEntity, Object entity) {
		if (null == clzesEnabled2Join) {
			return;
		}
		List<FilterDescriptor> deps = clzesEnabled2Join.get(entity.getClass());
		for (FilterDescriptor fd : deps) {
			Object depObj = fetchEntity.get(getAliasByTableName(getTableNameByClass(fd
					.getJoinDependency())));
			Object value = null;
			if (null == depObj || null == (value = getValue(fd.getJoinField(), depObj))) {
				continue;
			}
			Field pk = MetaData.getPrimaryKeyField(entity.getClass());
			if (getValue(pk, value).equals(getValue(pk, entity))) {
				setValue2Field(depObj, fd.getJoinField(), entity);
				return;
			}
		}
	}

	private void readEntity(ResultSet rs, Map<String, Object> fetchEntity,
							Map<String, Object> joinFetchEntity) throws SQLException {
		for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
			Object colValue = rs.getObject(i);
			if (null == colValue) {
				continue;
			}
			if (sessionFactory.getDialectType().isOracle() && colValue instanceof Date) {
				colValue = rs.getTimestamp(i);
			}
			String[] colNames = colHeaders.get(i - 1);
			if (2 == colNames.length) {
				readFetchEntity(fetchEntity, colValue, colNames);
			} else if (3 == colNames.length) {
				readMany2ManyEntity(joinFetchEntity, colValue, colNames);
			}
		}
	}

	private void readMany2ManyEntity(Map<String, Object> entityMap, Object colValue, String[] colNames) {
		// joinFetch出来的数据
		String tableAlias = colNames[1];
		String tableName = getTableNameByAlias(tableAlias);
		String fieldNameAlias = colNames[2];
		String fieldName = MetaData.getFieldsAliasMapping(
				MetaData.getClassByTableName(tableName)).get(fieldNameAlias);
		if (null == entityMap.get(tableName)) {
			Object entity = DMLObject.newInstance(MetaData.getClassByTableName(tableName));
			entityMap.put(tableName, entity);
		}
		Field field = getFieldByName(tableName, fieldName);
		setValue2EntityField(entityMap, colValue, tableName, field);
	}

	private void setValue2EntityField(Map<String, Object> entityMap, Object colValue, String tableName, Field field) {
		if (MetaData.isEntityClass(field.getType())) {
			Object newInstance = DMLObject.newInstance(field.getType());
			Field pkField = MetaData.getPrimaryKeyField(field.getType());
			setValue2EntityField(newInstance, pkField, colValue);
			setValue2Field(entityMap.get(tableName), field, newInstance);
		} else {
			setValue2EntityField(entityMap.get(tableName), field, colValue);
		}
	}

	/**
	 *
	 * 通过别名获取表名
	 * @param alias
	 * @return
	 *
	 */
	protected String getTableNameByAlias(String alias) {
		Iterator<String> it = aliasMapping.keySet().iterator();
		while (it.hasNext()) {
			String tableName = it.next();
			if (alias.equals(aliasMapping.get(tableName))) {
				return tableName;
			}
		}
		return "";
	}

	private void readFetchEntity(Map<String, Object> entityMap, Object colValue, String[] colNames) {
		String tableAlias = colNames[0]; // 带后缀的表别名
		String realTableAlias = tableAlias; // 真实表别名
		boolean isMultiFetch = tableAlias.contains(UNDERLINE);
		if (isMultiFetch) {
			realTableAlias = realTableAlias.split(UNDERLINE)[0];
		}
		String tableName = getTableNameByAlias(realTableAlias);
		String fieldNameAlias = colNames[1];
		if (null == entityMap.get(tableAlias)) {
			Object entity = DMLObject.newInstance(MetaData.getClassByTableName(tableName));
			entityMap.put(tableAlias, entity);
		}
		Field field;
		try {
			String fieldName = MetaData.getFieldsAliasMapping(MetaData.getClassByTableName(tableName)).get(fieldNameAlias);
			field = getFieldByName(tableName, fieldName);
		} catch (Exception e) {
			throw new RabbitDMLException(e);
		}
		setValue2EntityField(entityMap, colValue, tableAlias, field);
	}

	protected Field getFieldByName(String tableName, String fieldName) {
		Class<?> clz = MetaData.getClassByTableName(tableName);
		return checkField(clz, fieldName);
	}

	/**
	 * <b>Description  给指定对象的字段设值</b>
	 * @param target
	 * @param field
	 * @param value
	 */
	protected void setValue2EntityField(Object target, Field field, Object value) {
		Object gotValue = value;
		if (isEnableGetFilter()) {
			gotValue = sessionFactory.onValueGot(value, field);
		}
		getTransformer().setValue2EntityField(target, field, gotValue);
	}

	/**
	 * <b>Description  添加Or类型的过滤条件</b>
	 * @param multiDropFilter
	 */
	public AbstractQuery<T> addFilter(MultiDropFilter multiDropFilter) {
		cacheMultiDropFilter(multiDropFilter);
		return this;
	}

	/**
	 *
	 * <b>Description:	生成sql语句</b><br>	
	 *
	 */
	protected void createQuerySql() {
		reset2PreparedStatus();
		runCallBackTask();
		doFilterChecking();
		// 分析并准备查询条件
		prepareFilterMetas();
		combineFilters();
		convertJoinFilter2Metas();
		prepareMany2oneFilters();
		doShardingCheck();
		doOrderCheck();
		// 创建被查询的表字段sql片段
		createFieldsSql();
		transformFieldsSql();
		createFromSql();
		createJoinSql();
		createFilterSql();
		createGroupBySql();
		createOrderSql();
		createPageSql();
	}

	// 检查过滤条件是否冲突
	private void doFilterChecking() {
		List<Class<?>> list = new ArrayList<>();
		this.dmlFilters.values().forEach(f -> list.addAll(f.getAssociatedClass()));
		list.forEach(clz -> {
			if (addedFilters.containsKey(clz) || addedJoinFilters.containsKey(clz)) {
				throw new ConflictFilterException(clz);
			}
		});
	}

	/**
	 * <b>@description 添加groupBy字段 </b>
	 * @param fields
	 * @return
	 */
	public abstract AbstractQuery<T> groupBy(String... fields);

	/**
	 * <b>@description 创建groupby sql片段 </b>
	 */
	protected void createGroupBySql() {

	}

	/**
	 * <b>Description  重置到执行sql之前的状态 </b>
	 */
	protected void reset2PreparedStatus() {
		if (run) {
			sql = new StringBuilder();
			filterDescriptors.clear();
			many2oneFilterDescriptors.clear();
			restClassesEnabled2Join();
			preparedValues.clear();
			factors.clear();
			addedFilters.clear();
		}
	}

	/**
	 * <b>Description  分区表检查</b>
	 */
	private void doShardingCheck() {
		if (!isShardingOperation()) {
			return;
		}
		metaData.updateTableName(getCurrentShardedTableName(getFactors()));
		filterDescriptors.clear();
		many2oneFilterDescriptors.clear();
		restClassesEnabled2Join();
		prepareFilterMetas();
		combineFilters();
		prepareMany2oneFilters();
	}

	private void restClassesEnabled2Join() {
		clzesEnabled2Join = null;
		//刷新classesEnabled2Join对象的值
		getClzesEnabled2Join();
		resetDependencyPathTableAlias();
	}

	/**
	 * <b>Description  重置依赖路径中表的别名</b>
	 */
	private void resetDependencyPathTableAlias() {
		for (Entry<Class<?>, List<FilterDescriptor>> entry : dependencyPath.entrySet()) {
			if (!clzesEnabled2Join.containsKey(entry.getKey())) {
				continue;
			}
			for (FilterDescriptor fdc : clzesEnabled2Join.get(entry.getKey())) {
				resetDepPathTableAliasByDescriptor(entry, fdc);
			}
		}
	}

	private void resetDepPathTableAliasByDescriptor(
			Entry<Class<?>, List<FilterDescriptor>> entry, FilterDescriptor fdc) {
		for (FilterDescriptor fd : entry.getValue()) {
			if (fd.getField().equals(fdc.getField())) {
				fd.setKey(fdc.getKey());
			}
		}
	}

	@Override
	protected List<ShardFactor> getFactors() {
		if (!factors.isEmpty()) {
			return factors;
		}
		List<FilterDescriptor> mfds = getMainFilterDescriptors();
		for (FilterDescriptor fd : mfds) {
			factors.add(new ShardFactor(fd.getField(), fd.getFilter(), fd.getValue()));
		}
		return factors;
	}

	/**
	 *
	 * <b>Description:  检查order条件的合法性</b><br>.
	 *
	 */
	private void doOrderCheck() {
		for (Class<?> clz : asc.keySet()) {
			if (!isValidOrderOperation(clz)) {
				throw new OrderAssociationException(MetaData.getTableNameByClass(clz));
			}
		}
		for (Class<?> clz : desc.keySet()) {
			if (!isValidOrderOperation(clz)) {
				throw new OrderAssociationException(MetaData.getTableNameByClass(clz));
			}
		}
	}

	private boolean isValidOrderOperation(Class<?> clz) {
		if (clz.equals(getMetaData().getEntityClz())) {
			return true;
		}
		if (entity2Fetch.contains(clz)) {
			return true;
		}
		for (JoinFieldMetaData<?> jfm : joinFieldMetas) {
			if (clz.equals(jfm.getJoinClass())) {
				return true;
			}
		}
		for (DMLFilter filter : dmlFilters.values()) {
			if (filter.getAssociatedClass().contains(clz)) {
				return true;
			}
		}
		return false;
	}

	/**
	 *
	 * <b>Description:	构建分页sql片段</b><br>	
	 *
	 */
	protected void createPageSql() {
		if (!page) {
			return;
		}
		DialectTransformer transformer = getTransformer();
		sql = transformer.createPageSql(this);
	}

	/**
	 *
	 * <b>Description:	创建排序sql</b><br>	
	 *
	 */
	protected void createOrderSql() {
		DialectTransformer transformer = getTransformer();
		sql.append(transformer.createOrderSql(this));
	}

	/**
	 *
	 * <b>Description:	生成过滤条件sql</b><br>	
	 *
	 */
	private void createFilterSql() {
		sql.append(generateFilterSql());
	}

	/**
	 *
	 * <b>Description:	创建inner join 和 left join的sql片段</b><br>	
	 *
	 */
	protected void createJoinSql() {

		//创建过滤条件内连接的sql
		createInnerJoinsql();

		//创建addInnerJoinFilter添加的内连接sql部分
		createDynamicInnerJoinSql();

		//创建many2one的sql，  fetch部分
		createLeftJoinSql();

		//创建一对多或者多对多部分的sql, joinFetch部分
		createJoinFetchSql();


	}

	private void createInnerJoinsql() {
		sql.append(generateInnerJoinsql());
	}

	private void createJoinFetchSql() {
		sql.append(" ");
		for (JoinFieldMetaData<?> jfm : joinFieldMetas) {
			if (dmlFilters.containsKey(jfm.getJoinClass())) {
				// 如果包含了动态innerJoinFilter 就忽略左连接
				continue;
			}
			if (jfm.getAnnotation() instanceof OneToMany) {
				sql.append(createOTMJoinSql(jfm));
			} else if (jfm.getAnnotation() instanceof ManyToMany) {
				sql.append(createMTMJoinSql(jfm));
			}
			// 动态添加的join条件
			sql.append(addDynamicFilterSql(jfm));
		}
	}

	private StringBuilder addDynamicFilterSql(JoinFieldMetaData<?> jfm) {
		return addDynamicFilterSql(jfm, addedJoinFilters);
	}

	/**
	 *
	 * <b>Description:	生成一对多的过滤sql</b><br>
	 * @param jfm
	 * @return
	 *
	 */
	private StringBuilder createOTMJoinSql(JoinFieldMetaData<?> jfm) {
		return createOTMJoinSql(jfm, true);
	}

	/**
	 *
	 * <b>Description:	生成多对多的过滤sql</b><br>
	 * @param jfm
	 * @return
	 *
	 */
	private StringBuilder createMTMJoinSql(JoinFieldMetaData<?> jfm) {
		return createMTMJoinSql(jfm, true);
	}

	private void createLeftJoinSql() {
		for (FilterDescriptor fd : many2oneFilterDescriptors) {
			boolean innered = false;
			for (FilterDescriptor fdi : filterDescriptors) {
				if (fd.getFilterTable().equals(fdi.getFilterTable())) {
					innered = true;
					break;
				}
			}
			List<Class<?>> list = new ArrayList<>();
			this.dmlFilters.values().forEach(f -> list.addAll(f.getAssociatedClass()));
			if (list.contains(fd.getField().getType())) {
				innered = true;
			}
			if (!innered) {
				sql.append(LEFT_JOIN + fd.getFilterTable() + " "
						+ getTableAlias(fd));
				sql.append(" ON " + fd.getKey() + fd.getFilter()
						+ fd.getValue());
			}
		}
	}

	private String getTableAlias(FilterDescriptor fd) {
		String alias = getAliasByTableName(fd.getFilterTable());
		if (fd.isMultiFetchField()) {
			return alias + UNDERLINE + fd.getIndex();
		}
		return alias;
	}

	/**
	 * 创建Many2Many/One2Many的innerJoin sql
	 */
	private void createDynamicInnerJoinSql() {
		for (DMLFilter filter : dmlFilters.values()) {
			sql.append(filter.getJoinSql());
		}
	}

	/**
	 *
	 * <b>Description:	添加动态添加的过滤条件</b><br>
	 * @param jfm
	 * @param addedJoinFilters
	 *
	 */
	public StringBuilder addDynamicFilterSql(JoinFieldMetaData<?> jfm,
				Map<Class<?>, Map<String, List<DynamicFilterDescriptor>>> addedJoinFilters) {
		Class<?> clz = jfm.getJoinClass();
		if (!addedJoinFilters.containsKey(clz)) {
			return new StringBuilder();
		}
		return createDynamicFilterByClz(clz, addedJoinFilters.get(clz));
	}

	/**
	 * <b>@description 根据过滤条件创建关联sql </b>
	 * @param clz	被关联的实体
	 * @param map	过滤条件
	 * @return
	 */
	public StringBuilder createDynamicFilterByClz(Class<?> clz, Map<String, List<DynamicFilterDescriptor>> map) {
		StringBuilder sql = new StringBuilder();
		Iterator<String> fields = map.keySet().iterator();
		while (fields.hasNext()) {
			String field = fields.next();
			List<DynamicFilterDescriptor> list = map.get(field);
			for (DynamicFilterDescriptor dfd : list) {
				FieldMetaData fmd = MetaData.getCachedFieldsMeta(clz, field);
				sql.append("AND ");
				String key = getAliasByTableName(MetaData.getTableNameByClass(clz)) + "." + getColumnName(fmd.getColumn());
				String filter = dfd.getFilter().value();
				if (dfd.isReg()) {
					key = replaceRegByColumnName(dfd.getKeyReg(), fmd.getField(), key);
				}
				Object holderValue = RabbitValueConverter.convert(dfd.getValue(), fmd, dfd.isReg());
				if (FilterType.IS.value().equals(filter.trim())
						|| FilterType.IS_NOT.value().equals(filter.trim())) {
					sql.append(key + " " + filter + NULL);
				} else {
					cachePreparedValues(holderValue, fmd.getField());
					sql.append(key + " " + filter + " "
							+ createPlaceHolder(filter, holderValue) + " ");
				}
			}
		}
		return sql;
	}

	/**
	 *
	 * 生成多对多的过滤sql
	 * @param jfm
	 * @param leftJoin  是左连接?
	 * @return
	 *
	 */
	public StringBuilder createMTMJoinSql(JoinFieldMetaData<?> jfm, boolean leftJoin) {
		StringBuilder sb = new StringBuilder();
		ManyToMany mtm = (ManyToMany) jfm.getAnnotation();
		// 中间表别名
		String middleTableAias = getAliasByTableName(mtm.joinTable());
		// 多端表的别名
		String joinTableAlias = getAliasByTableName(jfm.getTableName());
		sb.append((leftJoin ? LEFT_JOIN : INNER_JOIN) + mtm.joinTable() + " " + middleTableAias + " ON ");
		sb.append(getAliasByTableName(getTableNameByClass(jfm.getTargetClass())) + "."
				+ MetaData.getPrimaryKey(jfm.getTargetClass(), sessionFactory) + " = ");
		sb.append(middleTableAias + "." + mtm.joinColumn() + " ");
		if (!StringUtils.isEmpty(mtm.filterColumn()) && joinColumnFilterValueMap.containsKey(jfm.getJoinClass())) {
			cachePreparedValues(joinColumnFilterValueMap.get(jfm.getJoinClass()), null);
			sb.append(" AND " + middleTableAias + "." + mtm.filterColumn() + " = " + PLACE_HOLDER);
		}
		sb.append((leftJoin ? LEFT_JOIN : INNER_JOIN) + jfm.getTableName() + " " + joinTableAlias + " ON ");
		sb.append(middleTableAias + "." + mtm.reverseJoinColumn() + " = ");
		sb.append(joinTableAlias + "." + getColumnName(jfm.getPrimaryKey()));
		sb.append(createJoinFilterSqlSegment(jfm));
		sb.append(" ");
		return sb;
	}

	/**
	 *
	 * <b>Description:	生成一对多的过滤sql</b><br>
	 * @param jfm
	 * @param leftJoin
	 * @return
	 *
	 */
	public StringBuilder createOTMJoinSql(JoinFieldMetaData<?> jfm, boolean leftJoin) {
		StringBuilder sb = new StringBuilder();
		OneToMany otm = (OneToMany) jfm.getAnnotation();
		String lj = leftJoin ? LEFT_JOIN : INNER_JOIN;
		sb.append(lj + jfm.getTableName() + " " + getAliasByTableName(jfm.getTableName()) + " ON ");
		sb.append(getAliasByTableName(getTableNameByClass(jfm.getTargetClass())) + "."
				+ MetaData.getPrimaryKey(jfm.getTargetClass(), sessionFactory) + " = ");
		sb.append(getAliasByTableName(jfm.getTableName()) + "." + otm.joinColumn());
		sb.append(createJoinFilterSqlSegment(jfm));
		sb.append(" ");
		return sb;
	}

	/**
	 * <b>Description 添加一对多、多对多的过滤条件部分sql </b>
	 * @param jfm
	 */
	private StringBuilder createJoinFilterSqlSegment(JoinFieldMetaData<?> jfm) {
		StringBuilder sb = new StringBuilder();
		List<FieldMetaData> filterMetas = getNonEmptyFieldMetas(
				jfm.getFilter(), jfm.getJoinClass());
		for (FieldMetaData fmd : filterMetas) {
			sb.append(createJoinFilterSqlSegmentByMeta(jfm, fmd));
		}
		return sb;
	}

	private StringBuilder createJoinFilterSqlSegmentByMeta(JoinFieldMetaData<?> jfm, FieldMetaData fmd) {
		StringBuilder sb = new StringBuilder();
		if (fmd.isForeignKey()) {
			if (fmd.getFieldValue().getClass().equals(getEntityClz())) {
				return sb;
			}
			Field pk = MetaData.getPrimaryKeyField(fmd.getFieldValue().getClass());
			Object pkv = getValue(pk, fmd.getFieldValue());
			if (null == pkv) {
				return sb;
			}
			String key = getAliasByTableName(jfm.getTableName()) + "."
					+ getColumnName(fmd.getColumn());
			String filter = FilterType.EQUAL.value();
			sb.append(AND + key);
			Object hv = RabbitValueConverter.convert(pkv, fmd);
			cachePreparedValues(hv, fmd.getField());
			sb.append(" " + filter + " " + createPlaceHolder(filter, hv));
		} else {
			String key = getAliasByTableName(jfm.getTableName()) + "."
					+ getColumnName(fmd.getColumn());
			String filter = FilterType.EQUAL.value();
			sb.append(AND + key);
			Object hv = RabbitValueConverter.convert(fmd.getFieldValue(),
					fmd);
			cachePreparedValues(hv, fmd.getField());
			sb.append(" " + filter + " " + createPlaceHolder(filter, hv));
		}
		return sb;
	}

	/**
	 *
	 * 创建from语句
	 * @throws SQLException
	 *
	 */
	protected void createFromSql() {
		sql.append(" FROM " + metaData.getTableName() + " " + getAliasByTableName(metaData.getTableName()));
	}
	/**
	 *
	 * <b>Description:	根据db的不同，对字段片段的sql部分进行包装</b><br>	
	 *
	 */
	protected void transformFieldsSql() {
		DialectTransformer transformer = getTransformer();
		sql = transformer.completeFieldsSql(this);
	}

	/**
	 *
	 * <b>Description:	将动态添加的过滤条件转换成meta信息</b><br>	
	 *
	 */
	private void convertJoinFilter2Metas() {
		Iterator<Class<?>> it = addedJoinFilters.keySet().iterator();
		while (it.hasNext()) {
			Class<?> clz = it.next();
			convertByClass(clz);
		}
	}

	private void convertByClass(Class<?> enc) {
		for (JoinFieldMetaData<?> jfm : metaData.getJoinMetas()) {
			if (!enc.equals(jfm.getJoinClass())) {
				continue;
			}
			if (!isExistsJoinFieldMeta(enc)) {
				joinFieldMetas.add(jfm);
				return;
			}
		}
	}

	private boolean isExistsJoinFieldMeta(Class<?> enc) {
		for (JoinFieldMetaData<?> jfme : joinFieldMetas) {
			if (jfme.getJoinClass().equals(enc)) {
				return true;
			}
		}
		return false;
	}

	public AbstractQuery<T> distinct() {
		this.distinct = true;
		return this;
	}

	/**
	 *
	 * <b>Description:	创建被查询的表字段sql片段</b><br>	
	 *
	 */
	protected void createFieldsSql() {
		if (distinct) {
			sql.append(" DISTINCT ");
		}
		//主表字段
		sql.append(createFieldsSqlByMetas(getEntityClz()));

		//many2one关联表字段
		for (Class<?> clz : entity2Fetch) {
			if (clz.equals(getEntityClz())) {
				continue;
			}
			sql.append(createFieldsSqlByMetas(clz));
		}
		//多对多、一对多关联表字段
		for (JoinFieldMetaData<?> jfm : joinFieldMetas) {
			sql.append(createFieldsSqlByJoinMetas(jfm));
		}
		sql.deleteCharAt(sql.lastIndexOf(","));
	}

	/**
	 *
	 * <b>Description:	根据JoinFieldMetaData创建字段sql片段</b><br>
	 * @param jfm
	 * @return
	 *
	 */
	private StringBuilder createFieldsSqlByJoinMetas(JoinFieldMetaData<?> jfm) {
		Collection<FieldMetaData> fieldsMetas = MetaData.getCachedFieldsMetas(jfm.getJoinClass()).values();
		StringBuilder sb = new StringBuilder();
		Map<String, String> aliasMappings = MetaData.getFieldsAliasMapping(jfm.getJoinClass());
		for (FieldMetaData fmd : fieldsMetas) {
			boolean dynamic = fmd.getColumn().dynamic();
			if ((isForbiddenDynamic() && dynamic) ||
					!isConcernedField(jfm.getJoinClass(), fmd)) {
				continue;
			}
			String fn = fmd.getField().getName();
			String alias = aliasMappings.get(fn);
			String tableAlias = getAliasByTableName(jfm.getTableName());
			sb.append(appendColumnName(dynamic, tableAlias, getColumnName(fmd.getColumn()), jfm.getJoinClass()));
			sb.append(" AS ");
			sb.append("J" + SEPARATOR + tableAlias + SEPARATOR + alias);
			sb.append(", ");
		}
		return sb;
	}

	/**
	 *
	 * <b>Description:	通过meta字段信息拼接需要被查询的字段的sql</b><br>
	 * @param clz
	 * @return
	 *
	 */
	private StringBuilder createFieldsSqlByMetas(Class<?> clz) {
		StringBuilder sb = new StringBuilder();
		Collection<FieldMetaData> fieldsMetas = MetaData.getCachedFieldsMetas(clz).values();
		Map<String, String> aliasMappings = MetaData.getFieldsAliasMapping(clz);
		for (FieldMetaData fmd : fieldsMetas) {
			boolean dynamic = fmd.getColumn().dynamic();
			if ((isForbiddenDynamic() && dynamic) ||
					!isConcernedField(clz, fmd)) {
				continue;
			}
			String fn = fmd.getField().getName();
			String alias = aliasMappings.get(fn);
			String tableAlias = getAliasByTableName(getTableNameByClass(clz));
			String columnName = getColumnName(fmd.getColumn());
			if (fetchTimesMappingTable.containsKey(clz) && fetchTimesMappingTable.get(clz) > 1) {
				for (int j = 1; j <= fetchTimesMappingTable.get(clz); j++) {
					sb.append(tableAlias + UNDERLINE + j + "." + columnName);
					sb.append(" AS ");
					sb.append(tableAlias + UNDERLINE + j  + SEPARATOR + alias);
					sb.append(", ");
				}
			} else {
				sb.append(appendColumnName(dynamic, tableAlias, columnName, clz));
				sb.append(" AS ");
				sb.append(tableAlias + SEPARATOR + alias);
				sb.append(", ");
			}
		}
		return sb;
	}

	/**
	 * <b>@description 向sql对象拼接字段信息 </b>
	 * @param dynamic	是否是动态字段
	 * @param tableAlias	表的别名
	 * @param columnName	字段名
	 * @param fieldClz		字段所属的类的class信息
	 */
	private StringBuilder appendColumnName(boolean dynamic, String tableAlias,
								  String columnName, Class<?> fieldClz) {
		StringBuilder sb = new StringBuilder();
		if (dynamic) {
			String field = getFieldByReg(columnName);
			if (columnName.equals(field)) {
				sb.append(columnName);
			} else {
				FieldMetaData fmd = getFieldMetaByFieldName(fieldClz, field);
				sb.append(replaceRegByColumnName(columnName, fmd.getField(), tableAlias + "." + getColumnName(fmd.getColumn())));
			}
		} else {
			sb.append(tableAlias + "." + columnName);
		}
		return sb;
	}

	/**
	 * <b>Description 是否是本次查询关心的字段 </b>
	 * @param clz
	 * @param fmd
	 * @return
	 * @author 肖乾斌
	 */
	private boolean isConcernedField(Class<?> clz, FieldMetaData fmd) {
		return !concernFields.containsKey(clz)
				|| concernFields.get(clz).contains(fmd.getField().getName());
	}

	/**
	 *
	 * <b>Description:	将fetch的filterdescriptor信息准备好, 
	 *     循环递归按照依赖关系调整过滤条件在many2oneFilterDescripters中的顺序
	 * </b>
	 *
	 */
	protected void prepareMany2oneFilters() {
		removeInvalidFetch();
		Iterator<Class<?>> it = fetchClasses.keySet().iterator();
		while (it.hasNext()) {
			prepareByClass(it.next());
		}
	}

	private void prepareByClass(Class<?> clz2Fetch) {
		// 执行mutifetch的class不允许继续向下操作
		List<FilterDescriptor> fds = getFilterDescriptorsByClzAndDep(clz2Fetch,
				fetchClasses.get(clz2Fetch));
		fetchTimesMappingTable.put(clz2Fetch, fds.size());
		for (FilterDescriptor fd : fds) {
			prepareByFilterDescriptor(clz2Fetch, fd);
		}
	}

	private void prepareByFilterDescriptor(Class<?> clz, FilterDescriptor fd) {
		FilterDescriptor descriptor = fd;
		putDescriptor2Head(descriptor);
		entity2Fetch.add(clz);
		Class<?> dep = descriptor.getJoinDependency();
		while (!getEntityClz().equals(dep)) {
			if (fetchClasses.containsKey(dep)) {
				List<FilterDescriptor> deps = getFilterDescriptorsByClzAndDep(
						descriptor.getJoinDependency(), fetchClasses.get(dep));
				assertAmbiguousDependency(descriptor, deps);
				descriptor = deps.get(0);
				putDescriptor2Head(descriptor);
				entity2Fetch.add(dep);
			} else {
				List<FilterDescriptor> deps = getClzesEnabled2Join().get(dep);
				assertAmbiguousDependency(descriptor, deps);
				descriptor = deps.get(0);
				putDescriptor2Head(descriptor);
				entity2Fetch.add(dep);
			}
			dep = descriptor.getJoinDependency();
		}
	}

	private void assertAmbiguousDependency(FilterDescriptor descriptor,
										   List<FilterDescriptor> deps) {
		if (deps.size() > 1) {
			throw new AmbiguousDependencyException(descriptor
					.getJoinField().getType(),
					descriptor.getJoinDependency());
		}
	}

	/**
	 *
	 * <b>Description:	通过clz和其依赖找出过滤描述符</b><br>
	 * @param clz
	 * @param dep
	 * @return
	 *
	 */
	private List<FilterDescriptor> getFilterDescriptorsByClzAndDep(Class<?> clz, Class<?> dep) {
		Map<Class<?>, List<FilterDescriptor>> clzesEnabled2Join = getClzesEnabled2Join();
		if (!clzesEnabled2Join.containsKey(clz)) {
			throw new InvalidFetchOperationException(
					"class[" + clz.getName() + "] can't be fetched from class[" + getEntityClz().getName() + "]");
		}
		List<FilterDescriptor> filters = new ArrayList<>();
		for (FilterDescriptor fd : clzesEnabled2Join.get(clz)) {
			if (fd.getJoinDependency().equals(dep)) {
				filters.add(fd);
			}
		}
		return filters;
	}

	private void putDescriptor2Head(FilterDescriptor fd) {
		for (FilterDescriptor f : many2oneFilterDescriptors) {
			if (f.getFilterTable().equals(fd.getFilterTable()) && f.getJoinField().equals(fd.getJoinField())) {
				many2oneFilterDescriptors.remove(f);
				break;
			}
		}
		many2oneFilterDescriptors.add(0, fd);
	}

	/**
	 *
	 * <b>Description:	清除掉错误的fetch逻辑</b><br>	
	 *
	 */
	private void removeInvalidFetch() {
		Iterator<Class<?>> it = fetchClasses.keySet().iterator();
		while (it.hasNext()) {
			Class<?> key = it.next();
			if (!getClzesEnabled2Join().containsKey(key)) {
				continue;
			}
			if (!isFetchEnabled(key)) {
				fetchClasses.remove(key);
			}
		}
	}

	private boolean isFetchEnabled(Class<?> key) {
		for (FilterDescriptor fd : getClzesEnabled2Join().get(key)) {
			if (fd.getJoinDependency().equals(fetchClasses.get(key))) {
				return true;
			}
		}
		return false;
	}
	/**
	 *
	 * 通过表名获取别名
	 * @param tableName
	 * @return
	 *
	 */
	@Override
	public String getAliasByTableName(String tableName) {
		if (SessionFactory.isEmpty(aliasMapping.get(tableName))) {
			String alias = generateTableAlias();
			aliasMapping.put(tableName, alias);
		}
		return aliasMapping.get(tableName);
	}

	/**
	 * <b>Description  生成别名</b>
	 * @return
	 */
	private String generateTableAlias() {
		Collection<String> alias = aliasMapping.values();
		for (int i = 0;; i++) {
			String suffix = "";
			if (0 != i) {
				suffix = Integer.toString(i);
			}
			for (char c = 'A'; c <= 'Z'; c++) {
				if (alias.contains((c + suffix).toUpperCase())) {
					continue;
				}
				return c + suffix;
			}
		}
	}

	/**
	 *
	 * <b>Description:	动态新增内连接过滤条件</b><br>
	 * @param reg
	 * @param value
	 * @param ft
	 * @param depsPath	依赖路径
	 * @return
	 *
	 */
	public abstract AbstractQuery<T> addFilter(String reg, Object value, FilterType ft, Class<?>... depsPath);

	/**
	 *
	 * <b>Description:	动态新增内连接过滤条件</b><br>
	 * @param reg
	 * @param value
	 * @param depsPath
	 * @return
	 *
	 */
	public abstract AbstractQuery<T> addFilter(String reg, Object value, Class<?>... depsPath);

	/**
	 *
	 * <b>Description:	新增空查询条件</b><br>
	 * @param reg
	 * @param isNull
	 * @param depsPath
	 * @return
	 *
	 */
	public abstract AbstractQuery<T> addNullFilter(String reg, boolean isNull, Class<?>... depsPath);

	/**
	 *
	 * <b>Description:	新增空查询条件</b><br>
	 * @param reg
	 * @param depsPath
	 * @return
	 *
	 */
	public abstract AbstractQuery<T> addNullFilter(String reg, Class<?>... depsPath);

	/**
	 * <b>@description 新增一个非空过滤条件 </b>
	 * @param reg
	 * @param depsPath
	 * @return
	 */
	public AbstractQuery<T> addNotNullFilter(String reg, Class<?>... depsPath) {
		return addNullFilter(reg, false, depsPath);
	}

	/**
	 *
	 * <b>Description:	新增多对多/一对多过滤条件</b><br>
	 * @param reg
	 * @param ft
	 * @param value
	 * @param target
	 * @return
	 *
	 */
	public abstract AbstractQuery<T> addJoinFilter(String reg, FilterType ft, Object value, Class<?> target);

	/**
	 *
	 * <b>Description:	新增多对多/一对多过滤条件</b><br>
	 * @param reg
	 * @param value
	 * @param target
	 * @return
	 *
	 */
	public abstract AbstractQuery<T> addJoinFilter(String reg, Object value, Class<?> target);
	
	/**
	 *
	 * <b>Description:  新增一对多/多对多/ 多对一  链接查询</b><br>.
	 *                  该方法会覆盖addJoinFilter、joinFetch函数中同表的过滤条件
	 * @param filter
	 * @return
	 *
	 */
	public AbstractQuery<T> addDMLFilter(DMLFilter filter) {
		filter.setParentEntityClz(getEntityClz());
		filter.setQuery(this);
		if (dmlFilters.containsKey(filter.getEntityClz())) {
			throw new RepeatedDMLFilterException(filter.getEntityClz());
		}
		dmlFilters.put(filter.getEntityClz(), filter);
		return this;
	}

	/**
	 *
	 * <b>Description:	设置需要升序的字段, 多次调用只有最后一次生效</b><br>
	 * @param fieldName
	 * @return
	 *
	 */
	public AbstractQuery<T> asc(String fieldName) {
		return asc(fieldName, getEntityClz());
	}

	/**
	 *
	 * <b>Description:	设置需要升序的字段</b><br>
	 * @param fieldName	升序的字段名
	 * @param targetClz	升序字段对应的实体
	 * @return
	 *
	 */
	public AbstractQuery<T> asc(String fieldName, Class<?> targetClz) {
		return orderBy(fieldName, targetClz, asc);
	}

	/**
	 * <b>@description 	排序 </b>
	 * @param fieldName	排序的字段
	 * @param targetClz	排序的表对象
	 * @param order		缓存排序数据的map
	 * @return
	 */
	private AbstractQuery<T> orderBy(String fieldName, Class<?> targetClz, Map<Class<?>, HashSet<String>> order) {
		if (null == order.get(targetClz)) {
			order.put(targetClz, new HashSet<>());
		}
		String columnName = getColumnNameByFieldAndClz(fieldName, targetClz);
		order.get(targetClz).add(columnName);
		return this;
	}

	/**
	 *
	 * <b>Description:	根据字段名和clz信息从缓存中获取对应的表字段名</b><br>
	 * @param fieldName
	 * @param targetClz
	 * @return
	 *
	 */
	private String getColumnNameByFieldAndClz(String fieldName, Class<?> targetClz) {
		Map<String, FieldMetaData> fms = MetaData.getCachedFieldsMetas(targetClz);
		if (fms.containsKey(fieldName)) {
			return getColumnName(fms.get(fieldName).getColumn());
		}
		throw new RabbitDMLException("field [" + fieldName + "] is not declared in " + targetClz.getName());
	}

	/**
	 *
	 * <b>Description:	设置需要降序排列的字段, 多次调用只有最后一次生效</b><br>
	 * @param fieldName
	 * @return
	 *
	 */
	public AbstractQuery<T> desc(String fieldName) {
		return desc(fieldName, getEntityClz());
	}

	/**
	 *
	 * <b>Description:	设置需要降序排列的字段, 多次调用只有最后一次生效</b><br>
	 * @param fieldName
	 * @param targetClz
	 * @return
	 *
	 */
	public AbstractQuery<T> desc(String fieldName, Class<?> targetClz) {
		return orderBy(fieldName, targetClz, desc);
	}

	/**
	 *
	 * <b>Description:	统计数据条数</b><br>
	 *
	 */
	public long count() {
		createCountSql();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = sessionFactory.getConnection(getEntityClz(), getCurrentTableName(), DMLType.SELECT);
			stmt = conn.prepareStatement(sql.toString());
			setPreparedStatementValue(stmt, null);
			showSql();
			rs = stmt.executeQuery();
			rs.next();
			return Long.parseLong(rs.getObject(1).toString());
		} catch (Exception e) {
			showUnMaskedSql(false);
			Session.flagException();
			throw new RabbitDMLException(e);
		} finally {
			closeResultSet(rs);
			closeStmt(stmt);
			closeConnection(conn);
			Session.clearException();
			setRun();
		}
	}

	public List<T> list() {
		return execute().list();
	}

	public T unique() {
		return execute().unique();
	}

	protected void closeResultSet(ResultSet rs) {
		if (null != rs) {
			try {
				rs.close();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	protected void createCountSql() {
		reset2PreparedStatus();
		runCallBackTask();
		doFilterChecking();
		prepareFilterMetas();
		combineFilters();
		prepareMany2oneFilters();
		doShardingCheck();
		generateCountSql();
		createFromSql();
		createJoinSql();
		createFilterSql();
		createGroupBySql();
	}

	protected void generateCountSql() {
		sql.append("SELECT COUNT(1) ");
	}

	/**
	 *
	 * <b>Description:	关联查询</b><br>
	 * @param clz
	 * @param dependency
	 * @return
	 *
	 */
	public AbstractQuery<T> fetch(Class<?> clz, Class<?>... dependency) {
		doShardedFetchChecking(clz, dependency);
		if (0 == dependency.length) {
			fetchEntity(clz);
		} else {
			fetchEntity(clz, dependency[0]);
			for (int i = 0; i < dependency.length; i++) {
				if (i + 1 == dependency.length) {
					break;
				}
				fetchEntity(dependency[i], dependency[i + 1]);
			}
		}
		return this;
	}

	private void doShardedFetchChecking(Class<?> clz, Class<?>... dependency) {
		for (Class<?> c : dependency) {
			checkShardedFetch(c);
		}
		checkShardedFetch(clz);
	}

	/**
	 * <b>Description  检查是否取了分区表</b>
	 * @param clz
	 */
	protected void checkShardedFetch(Class<?> clz) {
		Entity entity = clz.getAnnotation(Entity.class);
		if (null != entity && !getEntityClz().equals(clz)
				&& !ShardingPolicy.class.equals(entity.policy())) {
			throw new FetchShardEntityException(clz);
		}
	}

	/**
	 *
	 * <b>Description:	关联查询</b><br>
	 * @param clz
	 * @param dependency
	 * @return
	 *
	 */
	private AbstractQuery<T> fetchEntity(Class<?> clz, Class<?> dependency) {
		if (null == clz) {
			return this;
		}
		// 不能自己fetch自己
		if (getEntityClz().equals(clz)) {
			throw new CycleFetchException(clz);
		}
		if (null != fetchClasses.get(clz)
				&& !fetchClasses.get(clz).equals(dependency)) {
			throw new RepeatedFetchOperationException(clz, fetchClasses.get(clz),
					dependency);
		}
		fetchClasses.put(clz, dependency);
		return this;
	}

	private AbstractQuery<T> fetchEntity(Class<?> clz) {
		if (null == clz) {
			return this;
		}
		if (null != fetchClasses.get(clz)
				&& !fetchClasses.get(clz).equals(getEntityClz())) {
			throw new RepeatedFetchOperationException(clz, fetchClasses.get(clz),
					getEntityClz());
		}
		fetchClasses.put(clz, getEntityClz());
		return this;
	}

	/**
	 *
	 * 查询出一对多或者多对多的数据。
	 * @param entityClz
	 * @return
	 *
	 */
	public <E> AbstractQuery<T> joinFetch(Class<E> entityClz) {
		return joinFetch(entityClz, null);
	}

	/**
	 *
	 * <b>Description:	联合查询多端数据</b><br>
	 * @param entityClz
	 * @param filter	多端过滤条件
	 * @return
	 *
	 */
	public <E> AbstractQuery<T> joinFetch(Class<E> entityClz, E filter) {
		checkShardedFetch(entityClz);
		if (!isValidFetch(entityClz)) {
			throw new InvalidJoinFetchOperationException(entityClz,
					getEntityClz());
		}
		for (JoinFieldMetaData<?> jfmo : metaData.getJoinMetas()) {
			if (entityClz.equals(jfmo.getJoinClass())) {
				refreshJoinFieldMetas(entityClz, filter, jfmo);
				return this;
			}
		}
		return this;
	}

	/**
	 * 多端查询时指定中间表的过滤字段值
	 * @param filterValue	中间表的filterColumn的值
	 * @param entityClz
	 * @param <E>
	 * @return
	 */
	public <E> AbstractQuery<T> joinFetchByFilter(Object filterValue, Class<E> entityClz) {
		return joinFetchByFilter(filterValue, entityClz, null);
	}

	/**
	 * 多端查询时指定中间表的过滤字段值
	 * @param filterValue	中间表的filterColumn的值
	 * @param entityClz
	 * @param filter
	 * @param <E>
	 * @return
	 */
	public <E> AbstractQuery<T> joinFetchByFilter(Object filterValue, Class<E> entityClz, E filter) {
		joinColumnFilterValueMap.put(entityClz, filterValue);
		return joinFetch(entityClz, filter);
	}

	/**
	 * <b>Description  构建fetch操作.</b>
	 * @return
	 */
	public FetchDescriptor<T> buildFetch() {
		return new FetchDescriptor<>(this);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <E> void refreshJoinFieldMetas(Class<E> entityClz, E filter,
										   JoinFieldMetaData jfmo) {
		JoinFieldMetaData jfm = jfmo.clone();
		jfm.setFilter(filter);
		for (JoinFieldMetaData jfme : joinFieldMetas) {
			if (jfme.getJoinClass().equals(entityClz)) {
				joinFieldMetas.remove(jfme);
				break;
			}
		}
		joinFieldMetas.add(jfm);
	}

	private <E> boolean isValidFetch(Class<E> entityClz) {
		for (@SuppressWarnings("rawtypes") JoinFieldMetaData jfmo : metaData.getJoinMetas()) {
			if (!entityClz.equals(jfmo.getJoinClass())) {
				continue;
			}
			return true;
		}
		return false;
	}

	/**
	 * <b>Description  设置查询时的别名</b>
	 * @param entityClz
	 * @param alias
	 * @return
	 */
	public AbstractQuery<T> alias(Class<?> entityClz, String alias) {
		if (aliasMapping.containsValue(alias.toUpperCase())) {
			throw new RepeatedAliasException(alias);
		}
		aliasMapping.put(getTableNameByClass(entityClz), alias.toUpperCase());
		return this;
	}

}

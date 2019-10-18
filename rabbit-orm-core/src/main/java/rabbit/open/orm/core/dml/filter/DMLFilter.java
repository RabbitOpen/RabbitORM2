package rabbit.open.orm.core.dml.filter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.RepeatedDMLFilterException;
import rabbit.open.orm.core.dml.AbstractQuery;
import rabbit.open.orm.core.dml.CallBackTask;
import rabbit.open.orm.core.dml.Query;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;

/**
 * <b>@description 过滤器 </b>
 */
@SuppressWarnings("rawtypes")
public abstract class DMLFilter {

	// 主表实体类
	protected Class<?> parentEntityClz;

	// 关联的entity
	protected Class<?> entityClz;

	// 基于当前实体{entityClz}的次级过滤条件
	protected List<DMLFilter> filters = new ArrayList<>();

	// 对应的查询对象
	private AbstractQuery<?> query;

	private QueryLoader queryLoader;

	protected List<CallBackTask> tasks = new ArrayList<>();

	// task执行后会生成对应的joinFilter
	protected Map<Class<?>, Map<String, List<DynamicFilterDescriptor>>> joinFilters;

	// 默认是内连接
	private boolean inner = true;

	public DMLFilter(Class<?> entityClz, boolean inner) {
		super();
		this.entityClz = entityClz;
		this.inner = inner;
	}

	// 基于entityClz添加过滤条件
	@SuppressWarnings("unchecked")
	public DMLFilter add(DMLFilter filter) {
		boolean exits = !filters.stream().filter(f -> f.getEntityClz() == filter.entityClz).collect(Collectors.toList()).isEmpty();
		if (exits) {
			throw new RepeatedDMLFilterException(filter.entityClz);
		}
		filters.add(filter);
		filter.setParentEntityClz(entityClz);
		filter.queryLoader = () -> new Query(query.getSessionFactory(), entityClz) {

			/**
			 * 	重载获取别名方法，调用上级query的别名管理方法实现统一别名管理
			 */
			@Override
			public String getAliasByTableName(String tableName) {
				return query.getAliasByTableName(tableName);
			}

			/**
			 * 	重载cachePreparedValues方法，将jdbc存储过程的值存储到顶级query中
			 */
			@Override
			public void cachePreparedValues(Object value, Field field) {
				query.cachePreparedValues(value, field);
			}
		};
		return this;
	}

	// 获取关联条件的sql
	public abstract String getJoinSql();

	public boolean isInner() {
		return inner;
	}

	/**
	 * <b>@description 添加过滤条件 </b>
	 * @param fieldName 字段名
	 * @param value     字段值
	 * @return
	 */
	public DMLFilter on(String fieldName, Object value) {
		return on(fieldName, FilterType.EQUAL, value);
	}

	/**
	 * <b>@description 添加过滤条件 </b>
	 * @param fieldName 字段名
	 * @param filter    过滤条件
	 * @param value     字段值
	 * @return
	 */
	public abstract DMLFilter on(String fieldName, FilterType filter, Object value);

	protected void runCallTasks() {
		joinFilters = new HashMap<>();
		for (CallBackTask task : tasks) {
			task.run();
		}
	}

	public void setQuery(AbstractQuery<?> query) {
		this.query = query;
	}

	public void setParentEntityClz(Class<?> parentEntityClz) {
		this.parentEntityClz = parentEntityClz;
	}

	protected AbstractQuery<?> getQuery() {
		if (null != query) {
			return query;
		} else {
			query = queryLoader.getQuery();
			return query;
		}
	}

	protected Class<?> getParentEntityClz() {
		return parentEntityClz;
	}

	public Class<?> getEntityClz() {
		return entityClz;
	}

	private interface QueryLoader {
		AbstractQuery getQuery();
	}

	/**
	 * <b>@description 获取过滤器关联到的所有实体class </b>
	 * @return
	 */
	public List<Class<?>> getAssociatedClass() {
		List<Class<?>> list = new ArrayList<>();
		list.add(getEntityClz());
		filters.forEach(f -> list.addAll(f.getAssociatedClass()));
		return list;
	}
}

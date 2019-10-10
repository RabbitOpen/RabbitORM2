package rabbit.open.orm.core.dml.filter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.AbstractQuery;
import rabbit.open.orm.core.dml.CallBackTask;
import rabbit.open.orm.core.dml.Query;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;

/**
 * <b>@description 过滤器 </b>
 */
public abstract class DMLFilter {
	
	// 主表实体类
	protected Class<?> parentEntityClz;

	// 关联的entity
	protected Class<?> entityClz;
	
	// 基于当前实体{entityClz}的次级过滤条件
	protected DMLFilter filter;
	
	// 合并的filter
	protected List<DMLFilter> combinedFilters = new ArrayList<>();
	
	// 对应的查询对象
	private AbstractQuery<?> query;
	
	private QueryLoader queryLoader;
	
	protected List<CallBackTask> tasks = new ArrayList<>();
	
	// task执行后会生成对应的joinFilter
	protected Map<Class<?>, Map<String, List<DynamicFilterDescriptor>>> joinFilters;
	
	// 默认是内连接
	private boolean inner = true;
	
	public DMLFilter(Class<?> entityClz) {
		this(entityClz, true);
	}

	public DMLFilter(Class<?> entityClz, boolean inner) {
		super();
		this.entityClz = entityClz;
		this.inner = inner;
	}

	// 基于entityClz添加过滤条件
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public DMLFilter add(DMLFilter filter) {
		this.filter = filter;
		filter.setParentEntityClz(entityClz);
		filter.queryLoader = () -> new Query(query.getSessionFactory(), filter.entityClz) {

            /**
             * 	重载获取别名方法，调用上级query的别名管理方法实现统一别名管理
             */
            @Override
            public String getAliasByTableName(String tableName) {
                return DMLFilter.this.query.getAliasByTableName(tableName);
            }

            /**
             * 	重载cachePreparedValues方法，将jdbc存储过程的值存储到顶级query中
             */
            @Override
            public void cachePreparedValues(Object value, Field field) {
            	DMLFilter.this.query.cachePreparedValues(value, field);
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
	 * @param value		字段值
	 * @return
	 */
	public DMLFilter on(String fieldName, Object value) {
		return on(fieldName, FilterType.EQUAL, value);
	}
	
	/**
	 * <b>@description 添加过滤条件 </b>
	 * @param fieldName 字段名
	 * @param filter 	过滤条件
	 * @param value		字段值
	 * @return
	 */
	public abstract DMLFilter on(String fieldName, FilterType filter, Object value);
	
	protected void runCallTasks() {
		joinFilters = new HashMap<>();
		for (CallBackTask task : tasks) {
			task.run();
		}
		this.combinedFilters.forEach(f -> {
			f.runCallTasks();
			Map<String, List<DynamicFilterDescriptor>> map = f.joinFilters.get(getEntityClz());
			Map<String, List<DynamicFilterDescriptor>> parentMap = joinFilters.get(getEntityClz());
			map.forEach((k, v) -> {
				if (parentMap.containsKey(k)) {
					parentMap.get(k).addAll(v);
				} else {
					parentMap.put(k, v);
				}
			});
		});
	}
	
	/**
	 * 
	 * <b>@description 合并filter的过滤条件 </b>
	 * @param filter
	 * @return
	 */
	public DMLFilter combineFilter(DMLFilter filter) {
		this.combinedFilters.add(filter);
		return this;
	}
	
	public void setQuery(AbstractQuery<?> query) {
		this.query = query;
		if (null != filter) {
			filter.setQuery(query);
		}
	}
	
	public void setParentEntityClz(Class<?> parentEntityClz) {
		this.parentEntityClz = parentEntityClz;
	}
	
	public void setFilter(DMLFilter filter) {
		this.filter = filter;
	}
	
	protected AbstractQuery<?> getQuery() {
		if (null != queryLoader) {
			return queryLoader.getQuery();
		}
		return query;
	}
	
	protected Class<?> getParentEntityClz() {
		return parentEntityClz;
	}
	
	public Class<?> getEntityClz() {
		return entityClz;
	}
	
	private interface QueryLoader {
		AbstractQuery<?> getQuery();
	}
	
	public List<Class<?>> getAssociatedClass() {
		List<Class<?>> list = new ArrayList<>();
		list.add(getEntityClz());
		if (null != filter) {
			list.addAll(filter.getAssociatedClass());
		}
		return list;
	}
}

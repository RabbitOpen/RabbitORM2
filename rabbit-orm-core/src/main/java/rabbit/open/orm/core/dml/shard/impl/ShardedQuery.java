package rabbit.open.orm.core.dml.shard.impl;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.*;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.ShardedDMLObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * <b>@description 分片查询 </b>
 * @param <T>
 */
public class ShardedQuery<T> extends ShardedDMLObject<T> {

	protected Query<T> query;

	private int pageSize = 500;

	private Set<String> asc = new HashSet<>();
	
	private Set<String> desc = new HashSet<>();
	
	// 动态添加的分页过滤条件
	private DynamicFilterTask<T> filterTask = null;
	
	public ShardedQuery(SessionFactory factory, Class<T> clz, T filter) {
		query = new Query<T>(factory, filter, clz) {

			// 如果迭代器中有分区表则直接取当前分区表
			@Override
			protected TableMeta getCurrentShardedTableMeta(List<ShardFactor> factors) {
				return getCurrentShardedTable(factors);
			}

		};
		validDMLObject();
		setCursor(new QueryCursor<>(this, pageSize));
	}

	@Override
	protected DMLObject<T> getDMLObject() {
		return query;
	}
	
	/**
	 * <b>@description 返回一个数据加载游标 </b>
	 * @return
	 */
	public QueryCursor<T> cursor() {
		return (QueryCursor<T>) cursor;
	}
	
	public ShardedQuery<T> setPageSize(int pageSize) {
		this.pageSize = pageSize;
		((QueryCursor<T>)cursor).setPageSize(pageSize);
		return this;
	}

	public ShardedQuery<T> addFilter(String reg, Object value, FilterType ft) {
		query.addFilter(reg, value, ft);
		return this;
	}

	public ShardedQuery<T> addFilter(String reg, Object value) {
		return addFilter(reg, value, FilterType.EQUAL);
	}
	
	/**
	 * <b>@description 设置分页过滤条件 </b>
	 * @param reg
	 * @param value
	 * @param ft
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected ShardedQuery<T> setPageFilter(String reg, Object value, FilterType ft) {
		if (null == filterTask) {
			addFilter(reg, value, ft);
			List<CallBackTask> filterTasks = query.getFilterTasks();
			filterTask = (DynamicFilterTask<T>) filterTasks.get(filterTasks.size() - 1);
			return this;
		} else {
			filterTask.setValue(value);
			return this;
		}
	}

	/**
	 * <b>@description 移除分页过滤条件  </b>
	 * @return
	 */
	protected ShardedQuery<T> removePageFilter() {
		query.getFilterTasks().remove(filterTask);
		filterTask = null;
		return this;
	}

	/**
	 * <b>@description 单表内部排序 </b>
	 * @param fieldName
	 * @return
	 */
	public ShardedQuery<T> asc(String fieldName) {
		query.asc(fieldName);
		asc.add(fieldName);
		return this;
	}

	/**
	 * <b>@description 判断字段是否已经排过升序 </b>
	 * @param fieldName
	 * @return
	 */
	protected boolean isAscOrdered(String fieldName) {
		return asc.contains(fieldName);
	}

	/**
	 * <b>@description 判断字段是否已经排过降序 </b>
	 * @param fieldName
	 * @return
	 */
	protected boolean isDescOrdered(String fieldName) {
		return desc.contains(fieldName);
	}
	
	/**
	 * <b>@description 单表内部排序 </b>
	 * @param fieldName
	 * @return
	 */
	public ShardedQuery<T> desc(String fieldName) {
		query.desc(fieldName);
		desc.add(fieldName);
		return this;
	}

	/**
	 * 
	 * <b>Description: 新增空查询条件</b><br>
	 * @param reg
	 * @param isNull
	 * @return
	 * 
	 */
	public ShardedQuery<T> addNullFilter(String reg, boolean isNull) {
		query.addFilter(reg, null, isNull ? FilterType.IS : FilterType.IS_NOT);
		return this;
	}
	
	public ShardedQuery<T> addNullFilter(String reg) {
		return addNullFilter(reg, true);
	}
}

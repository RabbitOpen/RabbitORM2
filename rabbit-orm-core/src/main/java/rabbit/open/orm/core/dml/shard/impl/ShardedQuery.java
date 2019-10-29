package rabbit.open.orm.core.dml.shard.impl;

import java.util.List;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.Query;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.ShardedDMLObject;

/**
 * 
 * <b>@description 分片查询 </b>
 * @param <T>
 */
public class ShardedQuery<T> extends ShardedDMLObject<T> {

	protected Query<T> query;

	private int pageSize = 500;

	public ShardedQuery(SessionFactory factory, Class<T> clz, T filter) {
		query = new Query<T>(factory, filter, clz) {

			// 如果迭代器中有分区表则直接取当前分区表
			@Override
			protected TableMeta getCurrentShardedTableMeta(List<ShardFactor> factors) {
				return getCurrentShardedTable(factors);
			}

		};
		validDMLObject();
		setCursor(new QueryCursor<>(query, pageSize));
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
	 * <b>@description 单表内部排序 </b>
	 * @param fieldName
	 * @return
	 */
	public ShardedQuery<T> asc(String fieldName) {
		query.asc(fieldName);
		return this;
	}

	/**
	 * <b>@description 单表内部排序 </b>
	 * @param fieldName
	 * @return
	 */
	public ShardedQuery<T> desc(String fieldName) {
		query.desc(fieldName);
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

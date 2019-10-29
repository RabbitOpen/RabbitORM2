package rabbit.open.orm.core.dml.shard.impl;

import java.util.List;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.Update;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.ShardedDMLObject;

/**
 * <b>@description 分片更新  </b>
 * @param <T>
 */
public class ShardedUpdate<T> extends ShardedDMLObject<T> {

	protected Update<T> update;

	public ShardedUpdate(SessionFactory factory, Class<T> clz, T filter) {
		update = new Update<T>(factory, filter, clz) {

			// 如果迭代器中有分区表则直接取当前分区表
			@Override
			protected TableMeta getCurrentShardedTableMeta(List<ShardFactor> factors) {
				return getCurrentShardedTable(factors);
			}

		};
		validDMLObject();
		setCursor(new UpdateCursor<>(update));
	}
	
	@Override
	protected DMLObject<T> getDMLObject() {
		return update;
	}
	
	public ShardedUpdate<T> addFilter(String reg, Object value, FilterType ft) {
		update.addFilter(reg, value, ft);
		return this;
	}

	public ShardedUpdate<T> addFilter(String reg, Object value) {
		return addFilter(reg, value, FilterType.EQUAL);
	}
	
	/**
	 * <b>@description 设置 </b>
	 * @param reg
	 * @param value
	 * @return
	 */
	public ShardedUpdate<T> set(String reg, Object value) {
		update.set(reg, value);
		return this;
	}
	
	/**
	 * 
	 * <b>Description:	设置需要更新的字段的值</b><br>
	 * @param value
	 * 
	 */
	public ShardedUpdate<T> setValue(T value) {
		update.setValue(value);
		return this;
	}
	
	/**
	 * <b>@description 设空 </b>
	 * @param fields
	 * @return
	 */
	public ShardedUpdate<T> setNull(String... fields) {
		update.setNull(fields);
		return this;
	}
	
	/**
	 * <b>@description 返回一个更新游标  </b>
	 * @return
	 */
	public UpdateCursor<T> cursor() {
		return (UpdateCursor<T>) cursor;
	}
	
	/**
	 * 
	 * <b>Description: 新增空过滤条件</b><br>
	 * @param reg
	 * @param isNull
	 * @return
	 * 
	 */
	public ShardedUpdate<T> addNullFilter(String reg, boolean isNull) {
		update.addFilter(reg, null, isNull ? FilterType.IS : FilterType.IS_NOT);
		return this;
	}

	public ShardedUpdate<T> addNullFilter(String reg) {
		return addNullFilter(reg, true);
	}
}

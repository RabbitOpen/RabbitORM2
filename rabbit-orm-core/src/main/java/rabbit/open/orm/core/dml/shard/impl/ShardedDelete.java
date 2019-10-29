package rabbit.open.orm.core.dml.shard.impl;

import java.util.List;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.Delete;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.ShardedDMLObject;

/**
 * <b>@description 分片删除  </b>
 * @param <T>
 */
public class ShardedDelete<T> extends ShardedDMLObject<T>  {

	protected Delete<T> delete;

	public ShardedDelete(SessionFactory factory, Class<T> clz, T filter) {
		delete = new Delete<T>(factory, filter, clz) {

			// 如果迭代器中有分区表则直接取当前分区表
			@Override
			protected TableMeta getCurrentShardedTableMeta(List<ShardFactor> factors) {
				return getCurrentShardedTable(factors);
			}

		};
		validDMLObject();
		setCursor(new DeleteCursor<>(delete));
	}
	
	@Override
	protected DMLObject<T> getDMLObject() {
		return delete;
	}
	
	public ShardedDelete<T> addFilter(String reg, Object value, FilterType ft) {
		delete.addFilter(reg, value, ft);
		return this;
	}

	public ShardedDelete<T> addFilter(String reg, Object value) {
		return addFilter(reg, value, FilterType.EQUAL);
	}
	
	/**
	 * <b>@description 返回一个删除游标  </b>
	 * @return
	 */
	public DeleteCursor<T> cursor() {
		return (DeleteCursor<T>) cursor;
	}
	
	/**
	 * 
	 * <b>Description: 新增空查询条件</b><br>
	 * @param reg
	 * @param isNull
	 * @return
	 * 
	 */
	public ShardedDelete<T> addNullFilter(String reg, boolean isNull) {
		delete.addFilter(reg, null, isNull ? FilterType.IS : FilterType.IS_NOT);
		return this;
	}
	
	public ShardedDelete<T> addNullFilter(String reg) {
		return addNullFilter(reg, true);
	}
}

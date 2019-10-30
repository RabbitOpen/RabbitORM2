package rabbit.open.orm.core.dml.shard.impl;

import java.util.List;

import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.Cursor;
import rabbit.open.orm.core.dml.shard.ShardedResultSetProcessor;

/**
 * 查询结果
 * @author 肖乾斌
 */
public abstract class AbstractQueryCursor<T> extends Cursor<T> {

	protected int pageSize;
	
	protected int pageIndex = 0;
	
	/**
	 * <b>@description  遍历数据 </b>
	 * @param processor
	 * @return
	 */
	public long next(ShardedResultSetProcessor<T> processor) {
		setHasNext(true);
		long count = 0;
		nextShardingTableMeta = null;
		while (hasNext()) {
			List<T> list = getDataList();
			TableMeta meta = getCurrentTableMeta();
			pageIndex++;
			if (list.size() < pageSize) {
				pageIndex = 0;
				int metaIndex = getMetaIndex(meta);
				if (metaIndex == getHittedTables().size() - 1) {
					setHasNext(false);
					nextShardingTableMeta = null;
				} else {
					nextShardingTableMeta = getHittedTables().get(metaIndex + 1);
				}
			} else {
				nextShardingTableMeta = meta;
			}
			count += list.size();
			processor.process(list, meta);
		}
		return count;
	}

	/**
	 * <b>@description 获取当前查询对应的表元信息 </b>
	 * @return
	 */
	protected abstract TableMeta getCurrentTableMeta();

	/**
	 * <b>@description 分页查询数据 </b>
	 * @return
	 */
	protected abstract List<T> getDataList();
	
	protected void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
}

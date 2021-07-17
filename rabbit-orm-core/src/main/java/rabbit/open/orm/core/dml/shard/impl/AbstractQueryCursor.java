package rabbit.open.orm.core.dml.shard.impl;

import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.Cursor;
import rabbit.open.orm.core.dml.shard.ShardedResultSetProcessor;

import java.util.List;

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
			TableMeta currentMeta = getCurrentTableMeta();
			pageIndex++;
			if (list.size() < pageSize) {
				pageIndex = 0;
				calNextShardingTableMeta(currentMeta);
			} else {
				nextShardingTableMeta = currentMeta;
			}
			count += list.size();
			processor.process(list, currentMeta);
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

package rabbit.open.orm.core.dml.shard.impl;

import java.util.List;

import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.Query;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.Cursor;
import rabbit.open.orm.core.dml.shard.ShardedResultSetProcessor;

/**
 * 查询结果
 * @author 肖乾斌
 */
public class QueryCursor<T> extends Cursor<T> {

	private Query<T> query;
	
	private int pageSize;
	
	private int pageIndex = 0;
	
	public QueryCursor(Query<T> query, int pageSize) {
		super();
		this.query = query;
		this.pageSize = pageSize;
	}
	
	@Override
	protected long getAffectedCount() {
		return query.count();
	}
	
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
			List<T> list = query.page(pageIndex, pageSize).list();
			TableMeta meta = query.getCurrentTableMeta();
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
	
	@Override
	protected DMLObject<T> getDMLObject() {
		return query;
	}
	
	protected void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
}

package rabbit.open.orm.core.dml;

import java.util.ArrayList;
import java.util.List;

import rabbit.open.orm.core.dml.shard.ShardedDataProcessor;
import rabbit.open.orm.core.dml.shard.ShardingPolicy;

/**
 * 查询结果
 * @author 肖乾斌
 */
public class Cursor<T> {

	private Query<T> query;
	
	private int pageSize;
	
	private int pageIndex = 0;
	
	// 下次查询使用的分区表
	private String nextShardingTable = null;
	
	// 所有符合条件的表
	private List<String> hittedTables = new ArrayList<>();
	
	private boolean hasNext = true;
	
	public Cursor(Query<T> query, int pageSize) {
		super();
		this.query = query;
		this.pageSize = pageSize;
	}
	
	protected boolean hasNext() {
		return hasNext;
	}
	
	/**
	 * <b>@description 计算总条数 </b>
	 * @return
	 */
	public long count() {
		hasNext = true;
		long count = 0L;
		nextShardingTable = null;
		while (hasNext()) {
			count += query.count();
			String currentTable = query.getMetaData().getTableName();
			ShardingPolicy shardingPolicy = query.getMetaData().getShardingPolicy();
			if (hittedTables.isEmpty()) {
				hittedTables = shardingPolicy.getAllHittedTables(query.getEntityClz(), query.getDeclaredTableName(), query.getFactors(), getAllTables());
			}
			if (hittedTables.indexOf(currentTable) == hittedTables.size() - 1) {
				hasNext = false;
				nextShardingTable = null;
			} else {
				nextShardingTable = hittedTables.get(hittedTables.indexOf(currentTable) + 1);
			}
		}
		return count;
	}
	
	/**
	 * <b>@description  遍历数据 </b>
	 * @param processor
	 * @return
	 */
	public long scanData(ShardedDataProcessor<T> processor) {
		hasNext = true;
		long count = 0;
		nextShardingTable = null;
		while (hasNext()) {
			List<T> list = query.page(pageIndex, pageSize).list();
			String currentTable = query.getMetaData().getTableName();
			pageIndex++;
			if (list.size() < pageSize) {
				pageIndex = 0;
				ShardingPolicy shardingPolicy = query.getMetaData().getShardingPolicy();
				if (hittedTables.isEmpty()) {
					hittedTables = shardingPolicy.getAllHittedTables(query.getEntityClz(), query.getDeclaredTableName(), query.getFactors(), getAllTables());
				}
				if (hittedTables.indexOf(currentTable) == hittedTables.size() - 1) {
					hasNext = false;
					nextShardingTable = null;
				} else {
					nextShardingTable = hittedTables.get(hittedTables.indexOf(currentTable) + 1);
				}
			} else {
				nextShardingTable = currentTable;
			}
			count += list.size();
			processor.process(list);
		}
		return count;
	}

	/**
	 * <b>@description 获取T对应的所有分区表 </b>
	 * @return
	 */
	private List<String> getAllTables() {
		return query.getSessionFactory().getShardedTablesCache().get(query.getMetaData().getShardingPolicy().getClass());
	}
	
	protected void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	/**
	 * <b>@description 获取下次查询使用的分区表  </b>
	 * @return
	 */
	protected String getNextShardingTable() {
		return nextShardingTable;
	}
}

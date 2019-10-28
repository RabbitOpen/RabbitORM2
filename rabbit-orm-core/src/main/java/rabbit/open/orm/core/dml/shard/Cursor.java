package rabbit.open.orm.core.dml.shard;

import java.util.ArrayList;
import java.util.List;

import rabbit.open.orm.core.dml.DMLObject;

public abstract class Cursor<T> {

	// 下次dml操作使用的分区表
	protected String nextShardingTable = null;

	// 所有符合条件的表
	private List<String> hittedTables = new ArrayList<>();
	
	private boolean hasNext = true;
	
	protected boolean hasNext() {
		return hasNext;
	}
	
	protected void setHasNext(boolean hasNext) {
		this.hasNext = hasNext;
	}
	
	/**
	 * <b>@description 计算总条数 </b>
	 * @return
	 */
	public final long count() {
		return count(null);
	}
	
	/**
	 * <b>@description 计算总条数 </b>
	 * @return
	 */
	/**
	 * <b>@description 计算总条数 </b>
	 * @return
	 */
	public long count(ShardedResultCounter counter) {
		setHasNext(true);
		long total = 0L;
		nextShardingTable = null;
		while (hasNext()) {
			long count = getAffectedCount();
			total += count;
			String currentTable = getDMLObject().getMetaData().getTableName();
			if (getHittedTables().indexOf(currentTable) == getHittedTables().size() - 1) {
				setHasNext(false);
				nextShardingTable = null;
			} else {
				nextShardingTable = getHittedTables().get(getHittedTables().indexOf(currentTable) + 1);
			}
			if (null != counter) {
				counter.count(count, currentTable);
			}
		}
		return total;
	}

	/**
	 * <b>@description 当前dml操作受影响的条数  </b>
	 * @return
	 */
	protected abstract long getAffectedCount();

	/**
	 * <b>@description 获取下次查询使用的分区表 </b>
	 * 
	 * @return
	 */
	public String getNextShardingTable() {
		return nextShardingTable;
	}
	
	/**
	 * <b>@description 当前cursor关联的dml对象 </b>
	 * @return
	 */
	protected abstract DMLObject<T> getDMLObject();
	
	/**
	 * <b>@description 获取T对应的所有分区表 </b>
	 * @return
	 */
	protected List<String> getAllTables() {
		DMLObject<T> dmlObject = getDMLObject();
		return dmlObject.getSessionFactory().getShardedTablesCache().get(dmlObject.getMetaData().getShardingPolicy().getClass());
	}
	
	/**
	 * <b>@description 获取被命中的表集合 </b>
	 * @return
	 */
	protected List<String> getHittedTables() {
		if (hittedTables.isEmpty()) {
			ShardingPolicy shardingPolicy = getDMLObject().getMetaData().getShardingPolicy();
			hittedTables = shardingPolicy.getHittedTables(getDMLObject().getEntityClz(), getDMLObject().getDeclaredTableName(), getDMLObject().getFactors(), getAllTables());
		}
		return hittedTables;
	}
}

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

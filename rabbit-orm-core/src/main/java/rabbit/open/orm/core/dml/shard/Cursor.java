package rabbit.open.orm.core.dml.shard;

import java.util.ArrayList;
import java.util.List;

import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.TableMeta;

public abstract class Cursor<T> {

	// 下次dml操作使用的分区表
	protected TableMeta nextShardingTableMeta = null;

	// 所有符合条件的表
	private List<TableMeta> hittedTables = new ArrayList<>();
	
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
	public long count(ShardedResultCounter counter) {
		setHasNext(true);
		long total = 0L;
		nextShardingTableMeta = null;
		while (hasNext()) {
			long count = getAffectedCount();
			total += count;
			TableMeta currentMeta = getDMLObject().getCurrentTableMeta();
			calNextShardingTableMeta(currentMeta);
			if (null != counter) {
				counter.count(count, currentMeta);
			}
		}
		return total;
	}

	/**
	 * <b>@description 计算下一次查询使用的表元信息 </b>
	 * @param currentMeta
	 */
	protected void calNextShardingTableMeta(TableMeta currentMeta) {
		int metaIndex = getMetaIndex(currentMeta);
		if (metaIndex == getHittedTables().size() - 1) {
			setHasNext(false);
			nextShardingTableMeta = null;
		} else {
			nextShardingTableMeta = getHittedTables().get(metaIndex + 1);
		}
	}

	protected int getMetaIndex(TableMeta meta) {
		int size = getHittedTables().size();
		for (int i = 0; i < size; i++) {
			TableMeta tableMeta = getHittedTables().get(i);
			if (tableMeta.getDataSource().equals(meta.getDataSource())
					&& tableMeta.getTableName().equals(meta.getTableName())) {
				return i;
			}
		}
		return 0;
	}

	/**
	 * <b>@description 当前dml操作受影响的条数  </b>
	 * @return
	 */
	protected abstract long getAffectedCount();

	/**
	 * <b>@description 获取下次查询使用的分区表 </b>
	 * @return
	 */
	public TableMeta getNextShardingTableMeta() {
		return nextShardingTableMeta;
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
	protected List<TableMeta> getTableMetas() {
		SessionFactory sessionFactory = getDMLObject().getSessionFactory();
		MetaData<T> metaData = getDMLObject().getMetaData();
		return sessionFactory.getTableMetas(metaData.getShardingPolicy().getClass(), metaData.getEntityClz());
	}
	
	/**
	 * <b>@description 获取被命中的表集合 </b>
	 * @return
	 */
	protected List<TableMeta> getHittedTables() {
		if (hittedTables.isEmpty()) {
			ShardingPolicy shardingPolicy = getDMLObject().getMetaData().getShardingPolicy();
			hittedTables = shardingPolicy.getHittedTables(getDMLObject().getEntityClz(), getDMLObject().getDeclaredTableName(), getDMLObject().getFactors(), getTableMetas());
		}
		return hittedTables;
	}
}

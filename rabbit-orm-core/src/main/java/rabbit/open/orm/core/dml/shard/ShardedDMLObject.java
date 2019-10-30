package rabbit.open.orm.core.dml.shard;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.execption.InvalidShardedQueryException;

/**
 * <b>@description 分片dml基类 </b>
 * @param <T>
 */
public abstract class ShardedDMLObject<T> {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected Cursor<T> cursor;

	/**
	 * <b>@description 检查dml操作是否合法 </b>
	 */
	protected void validDMLObject() {
		if (!getDMLObject().getMetaData().isShardingTable()) {
			throw new InvalidShardedQueryException(getDMLObject().getMetaData().getTableName());
		}
	}
	
	/**
	 * <b>@description 获取当前分区表操作对应的DML对象 </b>
	 * @return
	 */
	protected abstract DMLObject<T> getDMLObject();

	/**
	 * <b>@description 获取当前分区表 </b>
	 * @param factors
	 * @return
	 */
	protected TableMeta getCurrentShardedTable(List<ShardFactor> factors) {
		if (null != cursor.getNextShardingTableMeta()) {
			return cursor.getNextShardingTableMeta();
		}
		List<TableMeta> hitTables = getDMLObject().getShardingPolicy().getHitTables(getDMLObject().getEntityClz(),
				getDMLObject().getDeclaredTableName(), factors, getDMLObject().getTableMetas());
		return hitTables.get(0);
	}
	
	protected void setCursor(Cursor<T> cursor) {
		this.cursor = cursor;
	}

}

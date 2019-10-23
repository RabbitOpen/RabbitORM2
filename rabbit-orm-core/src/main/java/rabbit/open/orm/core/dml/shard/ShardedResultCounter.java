package rabbit.open.orm.core.dml.shard;

/**
 * <b>@description 分片表结果计数器 </b>
 */
public interface ShardedResultCounter {

	/**
	 * <b>@description  计数器 </b>
	 * @param count		数据条数
	 * @param tableName	表名
	 */
	void count(long count, String tableName);
}

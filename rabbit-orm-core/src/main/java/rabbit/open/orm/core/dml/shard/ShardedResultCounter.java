package rabbit.open.orm.core.dml.shard;

import rabbit.open.orm.core.dml.meta.TableMeta;

/**
 * <b>@description 分片表结果计数器 </b>
 */
public interface ShardedResultCounter {

	/**
	 * <b>@description  计数器 </b>
	 * @param count		数据条数
	 * @param tableMeta	表元信息
	 */
	void count(long count, TableMeta tableMeta);
}

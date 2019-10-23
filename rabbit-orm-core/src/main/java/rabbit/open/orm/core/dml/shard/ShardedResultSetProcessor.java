package rabbit.open.orm.core.dml.shard;

import java.util.List;

/**
 * <b>@description 分片表数据处理器 </b>
 */
public interface ShardedResultSetProcessor<T> {

	/**
	 * <b>@description  处理数据 </b>
	 * @param list		加载的数据
	 * @param tableName	表名
	 */
	void process(List<T> list, String tableName);
}

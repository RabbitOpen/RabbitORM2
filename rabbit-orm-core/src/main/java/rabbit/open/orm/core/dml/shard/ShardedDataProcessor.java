package rabbit.open.orm.core.dml.shard;

import java.util.List;

/**
 * <b>@description 分片表数据处理器 </b>
 */
public interface ShardedDataProcessor<T> {

	/**
	 * <b>@description 处理数据 </b>
	 * @param list	加载的数据
	 */
	void process(List<T> list);
}

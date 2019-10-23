package rabbit.open.orm.core.dml.shard;

import java.util.List;

/**
 * <b>@description 默认分表策略(不分表)  </b>
 */
public class DefaultShardingPolicy implements ShardingPolicy {

	@Override
	public List<String> getHittedTables(Class<?> clz, String declaredTableName, List<ShardFactor> factors,
			List<String> allTables) {
		return allTables;
	}

}

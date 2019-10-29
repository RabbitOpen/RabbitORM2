package rabbit.open.orm.core.dml.shard;

import java.util.List;

import rabbit.open.orm.core.dml.meta.TableMeta;

/**
 * <b>@description 默认分表策略(不分表)  </b>
 */
public class DefaultShardingPolicy implements ShardingPolicy {

	@Override
	public List<TableMeta> getHittedTables(Class<?> clz, String declaredTableName, List<ShardFactor> factors,
			List<TableMeta> allTables) {
		return allTables;
	}

}

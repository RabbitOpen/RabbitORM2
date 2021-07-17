package rabbit.open.orm.core.dml.shard;

import rabbit.open.orm.core.dml.meta.TableMeta;

import java.util.List;

/**
 * <b>@description 默认分表策略(不分表)  </b>
 */
public class DefaultShardingPolicy implements ShardingPolicy {

	@Override
	public List<TableMeta> getHitTables(Class<?> clz, String declaredTableName, List<ShardFactor> factors,
			List<TableMeta> allTables) {
		return allTables;
	}

}

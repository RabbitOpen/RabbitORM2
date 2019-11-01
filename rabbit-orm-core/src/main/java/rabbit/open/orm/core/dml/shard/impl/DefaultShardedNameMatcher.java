package rabbit.open.orm.core.dml.shard.impl;

import rabbit.open.orm.core.dml.shard.ShardedNameMatcher;

/**
 * <b>@description 默认分区表表名匹配器 </b>
 */
public class DefaultShardedNameMatcher implements ShardedNameMatcher {

	@Override
	public boolean match(Class<?> clz, String entityTableName, String nameInDB) {
		return nameInDB.toLowerCase().startsWith(entityTableName.toLowerCase());
	}

}

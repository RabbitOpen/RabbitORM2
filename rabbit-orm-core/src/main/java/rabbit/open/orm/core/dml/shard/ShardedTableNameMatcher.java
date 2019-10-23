package rabbit.open.orm.core.dml.shard;

/**
 * <b>@description 默认分区表表名匹配器 </b>
 */
public class ShardedTableNameMatcher {

	public boolean match(String entityTableName, String nameInDB) {
		return nameInDB.toLowerCase().startsWith(entityTableName.toLowerCase());
	}
}

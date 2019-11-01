package rabbit.open.orm.core.dml.shard;

/**
 * <b>@description 分区表表名匹配器 </b>
 */
public interface ShardedNameMatcher {

	public boolean match(Class<?> clz, String entityTableName, String nameInDB);
}

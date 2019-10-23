package rabbit.open.orm.core.dml.shard;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Description  默认分表策略（部分表）</b>
 */
public interface ShardingPolicy {
    
    /**
     * <b>@description 根据过滤条件命中的所有符合条件的切片表 </b>
     * @param clz
     * @param declaredTableName		
     * @param factors
     * @param allTables
     * @return
     */
    default List<String> getHittedTables(Class<?> clz, String declaredTableName, List<ShardFactor> factors, List<String> allTables) {
		return new ArrayList<>();
	}
    
}

package rabbit.open.orm.core.dml.shard;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Description  默认分表策略（部分表）</b>
 */
public interface ShardingPolicy {

    /**
     * <b>Description       		根据过滤条件命中的第一个切片表</b>
     * @param clz           		实体类
     * @param declaredTableName     原始表名
     * @param factors       		分片条件
     * @param allTables     		当前表所有的分区表名
     * @return              		真实的分片表名
     */
    String getFirstHittedTable(Class<?> clz, String declaredTableName, List<ShardFactor> factors, List<String> allTables);
    
    /**
     * <b>@description 根据过滤条件命中的所有符合条件的切片表 </b>
     * @param clz
     * @param declaredTableName		
     * @param factors
     * @param allTables
     * @return
     */
    default List<String> getAllHittedTables(Class<?> clz, String declaredTableName, List<ShardFactor> factors, List<String> allTables) {
		return new ArrayList<>();
	}
    
}

package rabbit.open.orm.core.dml.shard;

import java.util.List;

import rabbit.open.orm.core.dml.meta.TableMeta;

/**
 * <b>Description  默认分表策略（部分表）</b>
 */
public interface ShardingPolicy {
    
    /**
     * <b>@description 根据过滤条件命中的所有符合条件的切片表 </b>
     * @param clz
     * @param declaredTableName		
     * @param factors
     * @param tableMetas
     * @return
     */
    List<TableMeta> getHittedTables(Class<?> clz, String declaredTableName, List<ShardFactor> factors, List<TableMeta> tableMetas);
    
}

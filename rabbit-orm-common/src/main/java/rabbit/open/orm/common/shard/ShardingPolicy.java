package rabbit.open.orm.common.shard;

import java.util.List;

/**
 * <b>Description  默认分表策略（部分表）</b>
 */
public class ShardingPolicy {

    /**
     * <b>Description       根据过滤条件获取表切片</b>
     * @param clz           实体类
     * @param tableName     原始表名
     * @param factors       分片条件
     * @return              真实的分片表名
     */
    public String getShardingTable(Class<?> clz, String tableName, List<ShardFactor> factors) {
        return tableName;
    }
    
}

package sharding.test.table.policy;

import java.util.List;

import rabbit.open.orm.common.annotation.Entity;
import rabbit.open.orm.common.shard.ShardFactor;
import rabbit.open.orm.common.shard.ShardingPolicy;
import sharding.test.table.exception.UnKnownShardException;

/**
 * <b>Description 自定义分表策略</b>
 */
public class DemoShardingPolicy extends ShardingPolicy {

    // 单向递增的后缀
    long suffix = -1L;

    /**
     * 根据主键分表(奇偶 分表)
     */
    @Override
    public String getShardingTable(Class<?> clz, String tableName,
            List<ShardFactor> factors) {
        if (!ShardingPolicy.class.equals(clz.getAnnotation(Entity.class).policy())) {
            if (containShardFactor(factors)) {
                return getShardingTableName(tableName, factors);
            } else {
                // 该操作无法确认分表信息，应该从业务上规避
                throw new UnKnownShardException("无法定位分表的操作被指定了");
            }
        } else {
            return super.getShardingTable(clz, tableName, factors);
        }
    }

    /**
     * 根据分表条件获取对应的分区表
     * @param tableName
     * @param factors
     * @return
     */
    private String getShardingTableName(String tableName, List<ShardFactor> factors) {
        for (ShardFactor sf : factors) {
            if ("id".equals(sf.getField().getName())) {
                return tableName + (((Long) sf.getValue()).longValue() % 2);
            }
        }
        return null;
    }

    /**
     * 判断DML操作是否包含分表条件
     * @param factors
     * @return
     */
    private boolean containShardFactor(List<ShardFactor> factors) {
        for (ShardFactor sf : factors) {
            if ("id".equals(sf.getField().getName())) {
                return true;
            }
        }
        return false;
    }

}

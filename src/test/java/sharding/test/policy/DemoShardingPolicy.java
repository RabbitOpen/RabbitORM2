package sharding.test.policy;

import java.util.List;

import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.shard.ShardFactor;
import rabbit.open.orm.shard.ShardingPolicy;
import sharding.test.entity.ShardingUser;

/**
 * <b>Description  自定义分表策略</b>
 */
public class DemoShardingPolicy extends ShardingPolicy {
    
    //单向递增的后缀
    long suffix = -1L;
    
    /**
     * 根据主键分表(奇偶 分表)
     */
    @Override
    public String getShardingTable(Class<?> clz, String tableName, List<ShardFactor> factors) {
        if (ShardingUser.class.equals(clz)) {
            if (containShardFactor(factors)) {
                for (ShardFactor sf : factors) {
                    if ("id".equals(sf.getField().getName())) {
                        return tableName + (((Long)sf.getValue()).longValue() % 2);
                    }
                }
                throw new RabbitDMLException("无法定位分表的操作被指定了");
            } else {
                //该操作无法确认分表信息，应该从业务上规避
                throw new RabbitDMLException("无法定位分表的操作被指定了");
            }
        } else {
            return super.getShardingTable(clz, tableName, factors);
        }
    }
    
    @Override
    public void tableCreated(Class<?> clz, String tableName,
            List<ShardFactor> factors) {
        suffix = getTableSuffix(clz, tableName, factors);
    }

    @Override
    public boolean isTableExists(Class<?> clz, String tableName, List<ShardFactor> factors) {
        if (ShardingUser.class.equals(clz)) {
            return getTableSuffix(clz, tableName, factors) <= suffix;
        } else {
            return true;
        }
    }
    
    private long getTableSuffix(Class<?> clz, String tableName, List<ShardFactor> factors) {
        if (containShardFactor(factors)) {
            for (ShardFactor sf : factors) {
                if ("id".equals(sf.getField().getName())) {
                    return ((Long)sf.getValue()).longValue() % 2;
                }
            }
            return -1L;
        }
        return -1L;
    }
    
    private boolean containShardFactor(List<ShardFactor> factors) {
        for (ShardFactor sf : factors) {
            if ("id".equals(sf.getField().getName())) {
                return true;
            }
        }
        return false;
    }
    
    
}

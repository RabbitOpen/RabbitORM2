package sharding.test.table.policy;

import java.util.Arrays;
import java.util.List;

import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.DefaultShardingPolicy;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.ShardingPolicy;
import rabbit.open.orm.core.dml.shard.execption.UnKnownShardException;

/**
 * <b>Description 自定义分表策略</b>
 */
public class DemoShardingPolicy implements ShardingPolicy {

    // 单向递增的后缀
    long suffix = -1L;

    /**
     * 	根据主键分表(奇偶 分表)
     */
    @Override
    public List<TableMeta> getHitTables(Class<?> clz, String declaredTableName, List<ShardFactor> factors,
    		List<TableMeta> tableMetas) {
    	if (!DefaultShardingPolicy.class.equals(clz.getAnnotation(Entity.class).shardingPolicy())) {
            if (containShardFactor(factors)) {
                return Arrays.asList(getShardingTableName(declaredTableName, factors));
            } else {
                // 该操作无法确认分表信息，应该从业务上规避
                throw new UnKnownShardException("无法定位分表的操作被指定了");
            }
        } else {
            return Arrays.asList(new TableMeta(declaredTableName, null));
        }
    }

    /**
     * 根据分表条件获取对应的分区表
     * @param tableName
     * @param factors
     * @return
     */
    private TableMeta getShardingTableName(String tableName, List<ShardFactor> factors) {
        for (ShardFactor sf : factors) {
            if ("id".equals(sf.getField().getName())) {
                return new TableMeta(tableName + (((Long) sf.getValue()).longValue() % 2), null);
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

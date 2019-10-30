package rabbit.open.orm.core.dml.shard.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.ShardingPolicy;
import rabbit.open.orm.core.dml.shard.execption.NoShardTableException;

/**
 * <b>@description 主键的值hash code取模分片策略,，不支持动态扩容 </b>
 */
public class PrimaryKeyModShardingPolicy implements ShardingPolicy {
	
	/**
     * <b>@description 根据过滤条件命中的所有符合条件的切片表 </b>
     * @param clz				实体类
     * @param declaredTableName	类注解中声明的表名	
     * @param factors			分表因素
     * @param tableMetas		clz对应的数据库中的表集合
     * @return
     */
	@Override
	public List<TableMeta> getHitTables(Class<?> clz, String declaredTableName, List<ShardFactor> factors,
			List<TableMeta> tableMetas) {
		if (tableMetas.isEmpty()) {
			throw new NoShardTableException(clz);
		}
		List<ShardFactor> keyFactors = getShardFactor(clz, factors);
		if (keyFactors.isEmpty()) {
			return tableMetas;
		} else {
			Set<TableMeta> hitTables = new HashSet<>(tableMetas);
			for (ShardFactor f : keyFactors) {
				// 和命中的表取交集
				hitTables.retainAll(getHitTablesByFactor(tableMetas, f));
			}
			if (hitTables.isEmpty()) {
				// 如果没有命中任何一张表 则随便取一个
				hitTables.add(tableMetas.get(0));
			}
			return new ArrayList<>(hitTables);
		}
	}

	protected Set<TableMeta> getHitTablesByFactor(List<TableMeta> tableMetas, ShardFactor f) {
		Set<TableMeta> tables = new HashSet<>();
		if (FilterType.EQUAL.value().trim().equals(f.getFilter().trim())) {
			tables.add(tableMetas.get(f.getValue().hashCode() % tableMetas.size()));
		} else if (FilterType.IN.value().trim().equals(f.getFilter().trim())) {
			for (Object v : getValueList(f)) {
				tables.add(tableMetas.get(v.hashCode() % tableMetas.size()));
			}
		} else {
			tables.addAll(tableMetas);
		}
		return tables;
	}

	@SuppressWarnings("unchecked")
	private List<Object> getValueList(ShardFactor factor) {
		return (List<Object>) factor.getValue();
	}

	/**
	 * 
	 * <b>@description 获取当前查询条件中的分表因子 </b>
	 * @param clz
	 * @param factors
	 * @return
	 */
	private List<ShardFactor> getShardFactor(Class<?> clz, List<ShardFactor> factors) {
		List<ShardFactor> keyFactors = new ArrayList<>();
		Field field = MetaData.getPrimaryKeyField(clz);
		for (ShardFactor factor : factors) {
			if (field.equals(factor.getField())) {
				keyFactors.add(factor);
			}
		}
		return keyFactors;
	}

}
package rabbit.open.orm.core.dml.shard.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.ShardingPolicy;

/**
 * <b>@description 主键的值hash code取模分片策略，多库时表名不能重复 </b>
 */
public class PrimaryKeyModShardingPolicy implements ShardingPolicy {

	@Override
	public String getFirstHittedTable(Class<?> clz, String declaredTableName, List<ShardFactor> factors,
			List<String> allTables) {
		return getAllHittedTables(clz, declaredTableName, factors, allTables).get(0);
	}

	@Override
	public List<String> getAllHittedTables(Class<?> clz, String declaredTableName, List<ShardFactor> factors,
			List<String> allTables) {
		if (allTables.isEmpty()) {
			throw new RabbitDMLException("no sharded tables are found for [" + clz + "]");
		}
		ShardFactor factor = getShardFactor(clz, factors);
		if (null == factor) {
			return allTables;
		} else {
			Set<String> tables = new HashSet<>();
			if (FilterType.EQUAL.value().trim().equals(factor.getFilter().trim())) {
				tables.add(declaredTableName + onSuffixCreated(factor.getValue().hashCode() % allTables.size()));
			} else if (FilterType.IN.value().trim().equals(factor.getFilter().trim())) {
				for (Object v : getValueList(factor)) {
					tables.add(declaredTableName + onSuffixCreated(v.hashCode() % allTables.size()));
				}
			} else {
				tables.addAll(allTables);
			}
			List<String> list = new ArrayList<>();
			list.addAll(tables);
			return list;
		}
	}

	@SuppressWarnings("unchecked")
	private List<Object> getValueList(ShardFactor factor) {
		if (factor.getValue().getClass().isArray()) {
			Object[] values = (Object[]) factor.getValue();
			return Arrays.asList(values);
		} else {
			return (List<Object>) factor.getValue();
		}
	}

	/**
	 * 
	 * <b>@description 获取当前查询条件中的分表因子 </b>
	 * @param clz
	 * @param factors
	 * @return
	 */
	private ShardFactor getShardFactor(Class<?> clz, List<ShardFactor> factors) {
		Field pkFied = MetaData.getPrimaryKeyField(clz);
		for (ShardFactor factor : factors) {
			if (pkFied.equals(factor.getField())) {
				return factor;
			}
		}
		return null;
	}

	protected String onSuffixCreated(int suffix) {
		return String.format("_%04d", suffix);
	}

}

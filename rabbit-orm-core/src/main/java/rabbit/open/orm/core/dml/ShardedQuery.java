package rabbit.open.orm.core.dml;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.execption.InvalidShardedQueryException;

/**
 * 
 * <b>@description 分片查询 </b>
 * @param <T>
 */
public class ShardedQuery<T> {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected Query<T> query;

	protected SessionFactory factory;

	protected MetaData<T> metaData;

	private int pageSize = 500;

	private Cursor<T> cursor;

	public ShardedQuery(SessionFactory factory, Class<T> clz, T filter) {

		query = new Query<T>(factory, filter, clz) {

			// 如果迭代器中有分区表则直接取当前分区表
			@Override
			protected String getCurrentShardedTableName(List<ShardFactor> factors) {
				if (null != cursor.getNextShardingTable()) {
					return cursor.getNextShardingTable();
				}
				return getShardingPolicy().getFirstHittedTable(getEntityClz(), getDeclaredTableName(), factors,
						getAllTables());
			}

		};
		this.factory = factory;
		this.metaData = query.getMetaData();
		if (!this.metaData.isShardingTable()) {
			throw new InvalidShardedQueryException(metaData.getTableName());
		}
		cursor = new Cursor<>(query, pageSize);
	}

	/**
	 * <b>@description 返回一个数据加载游标 </b>
	 * @return
	 */
	public Cursor<T> cursor() {
		return cursor;
	}
	
	public ShardedQuery<T> setPageSize(int pageSize) {
		this.pageSize = pageSize;
		cursor.setPageSize(pageSize);
		return this;
	}

	public ShardedQuery<T> addFilter(String reg, Object value, FilterType ft) {
		query.addFilter(reg, value, ft);
		return this;
	}

	public ShardedQuery<T> addFilter(String reg, Object value) {
		return addFilter(reg, value, FilterType.EQUAL);
	}

	/**
	 * <b>@description 单表内部排序 </b>
	 * @param fieldName
	 * @return
	 */
	public ShardedQuery<T> asc(String fieldName) {
		query.asc(fieldName);
		return this;
	}

	/**
	 * <b>@description 单表内部排序 </b>
	 * @param fieldName
	 * @return
	 */
	public ShardedQuery<T> desc(String fieldName) {
		query.desc(fieldName);
		return this;
	}

	/**
	 * 
	 * <b>Description: 新增空查询条件</b><br>
	 * @param reg
	 * @param isNull
	 * @return
	 * 
	 */
	public ShardedQuery<T> addNullFilter(String reg, boolean isNull) {
		query.addFilter(reg, null, isNull ? FilterType.IS : FilterType.IS_NOT);
		return this;
	}
}

package rabbit.open.orm.core.dml.shard.impl;

import java.util.List;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.SQLQuery;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.ShardedDMLObject;

/**
 * <b>@description 分片命名查询 </b>
 * @param <T>
 */
public class ShardedSQLQuery<T> extends ShardedDMLObject<T> {

	private SQLQuery<T> sqlQuery;
	
	private int pageSize = 500;
	
	public ShardedSQLQuery(SessionFactory sessionFactory, Class<T> clz, String queryName) {
		sqlQuery = new SQLQuery<T>(sessionFactory, clz, queryName) {

			// 如果迭代器中有分区表则直接取当前分区表
			@Override
			protected TableMeta getShardedTableMeta(List<ShardFactor> factors) {
				return getCurrentShardedTable(factors);
			}
		};
		validDMLObject();
		setCursor(new SQLQueryCursor<>(sqlQuery, pageSize));
	}
	
	/**
	 * <b>@description 返回一个数据加载游标 </b>
	 * @return
	 */
	public SQLQueryCursor<T> cursor() {
		return (SQLQueryCursor<T>) cursor;
	}
	
	public ShardedSQLQuery<T> setPageSize(int pageSize) {
		this.pageSize = pageSize;
		((SQLQueryCursor<T>)cursor).setPageSize(pageSize);
		return this;
	}
	
	@Override
	protected DMLObject<T> getDMLObject() {
		return sqlQuery.getQuery();
	}

	/**
	 * <b>Description      单个设值</b>
	 * @param fieldAlias   字段在sql中的别名
	 * @param value        字段的值
	 * @param fieldName    字段在对应实体中的名字 
	 * @param filerType    过滤条件类型
	 * @return
	 */
	public ShardedSQLQuery<T> set(String fieldAlias, Object value, String fieldName, FilterType filerType) {
		sqlQuery.set(fieldAlias, value, fieldName, sqlQuery.getQuery().getEntityClz(), filerType);
	    return this;
	}

	public ShardedSQLQuery<T> set(String fieldAlias, Object value, FilterType filerType) {
		return set(fieldAlias, value, fieldAlias, filerType);
	}
	
	public ShardedSQLQuery<T> set(String fieldAlias, Object value) {
		return set(fieldAlias, value, FilterType.EQUAL);
	}
}

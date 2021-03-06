package rabbit.open.orm.core.dml.shard.impl;

import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.SQLQuery;
import rabbit.open.orm.core.dml.meta.TableMeta;

import java.util.List;

/**
 * <b>@description 分片命名查询游标 </b>
 * @param <T>
 */
public class SQLQueryCursor<T> extends AbstractQueryCursor<T> {

	private SQLQuery<T> query;

	public SQLQueryCursor(SQLQuery<T> query, int pageSize) {
		this.query = query;
		setPageSize(pageSize);
	}

	@Override
	protected long getAffectedCount() {
		return query.count();
	}

	@Override
	protected List<T> getDataList() {
		return query.page(pageIndex, pageSize).list();
	}

	@Override
	protected TableMeta getCurrentTableMeta() {
		return query.getQuery().getCurrentTableMeta();
	}

	@Override
	protected DMLObject<T> getDMLObject() {
		return query.getQuery();
	}
}

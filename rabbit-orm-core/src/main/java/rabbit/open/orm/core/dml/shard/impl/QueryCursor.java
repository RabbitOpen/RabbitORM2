package rabbit.open.orm.core.dml.shard.impl;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.Query;
import rabbit.open.orm.core.dml.meta.TableMeta;
import rabbit.open.orm.core.dml.policy.PagePolicy;
import rabbit.open.orm.core.utils.ClassHelper;

/**
 * 查询结果
 * @author 肖乾斌
 */
public class QueryCursor<T> extends AbstractQueryCursor<T> {

	private ShardedQuery<T> query;
	
	// 按索引段排序分页时的起始值
	private Object start;
	
	private int tableIndex = 0;
	
	public QueryCursor(ShardedQuery<T> query, int pageSize) {
		this.query = query;
		this.pageSize = pageSize;
	}
	
	@Override
	protected long getAffectedCount() {
		return getRealDMLObject().count();
	}
	
	@Override
	protected TableMeta getCurrentTableMeta() {
		return getRealDMLObject().getCurrentTableMeta();
	}

	@Override
	protected List<T> getDataList() {
		if (PagePolicy.DEFAULT.equals(getRealDMLObject().getMetaData().getPagePolicy())) {
			return getRealDMLObject().page(pageIndex, pageSize).list();
		} else {
			String fieldName = getRealDMLObject().getMetaData().getIndexOrderedField();
			if (!query.isAscOrdered(fieldName) && !query.isDescOrdered(fieldName)) {
				// 如果用户没排序，则使用唯一索引字段默认排个序
				query.asc(fieldName);
			}
			if (0 != pageIndex) {
				query.setPageFilter(fieldName, start, query.isAscOrdered(fieldName) ? FilterType.GT : FilterType.LT);
			} else {
				if (0 != tableIndex) {
					// 换表以后删除分页条件
					query.removePageFilter();
				}
				tableIndex++;
			}
			List<T> list = getRealDMLObject().page(0, pageSize).list();
			setStart(fieldName, list);
			return list;
		}
	}

	/**
	 * <b>@description 将查询结果中的最大/最小值设置为下一批次的分页起点 </b>
	 * @param fieldName
	 * @param list
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void setStart(String fieldName, List<T> list) {
		if (list.isEmpty()) {
			return;
		}
		List<Object> ids = list.stream().map(t -> {
			try {
				Field field = ClassHelper.getField(getRealDMLObject().getEntityClz(), fieldName);
				field.setAccessible(true);
				return field.get(t);
			} catch (Exception e) {
				throw new RabbitDMLException(e.getMessage());
			}
		}).collect(Collectors.toList());
		ids.sort((o1, o2) -> ((Comparable)o1).compareTo((Comparable)o2));
		if (query.isAscOrdered(fieldName)) {
			start = ids.get(list.size() - 1);
		} else {
			start = ids.get(0);
		}
	}
	
	@Override
	protected DMLObject<T> getDMLObject() {
		return query.getDMLObject();
	}
	
	@Override
	protected void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	private Query<T> getRealDMLObject() {
		return (Query<T>)query.getDMLObject();
	}
}

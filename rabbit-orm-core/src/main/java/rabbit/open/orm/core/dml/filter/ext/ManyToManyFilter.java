package rabbit.open.orm.core.dml.filter.ext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.InvalidJoinFilterException;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.filter.DMLFilter;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.core.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;

/**
 * <b>@description 多对多过滤器 </b>
 */
public class ManyToManyFilter extends DMLFilter {

	protected JoinFieldMetaData<?> joinFieldMetaData;

	public ManyToManyFilter(Class<?> entityClz) {
		this(entityClz, true);
	}
	
	public ManyToManyFilter(Class<?> entityClz, boolean inner) {
		super(entityClz, inner);
	}

	@Override
	public String getJoinSql() {
		runCallTasks();
		MetaData<?> parentMeta = MetaData.getMetaByClass(getParentEntityClz());
		List<JoinFieldMetaData<?>> metas = parentMeta.getJoinMetas().stream()
				.filter(meta -> meta.getJoinClass() == getEntityClz()).collect(Collectors.toList());
		if (metas.isEmpty()) {
			throw new InvalidJoinFilterException(getParentEntityClz(), getEntityClz());
		}
		joinFieldMetaData = metas.get(0);
		StringBuilder sql = createJoinSql();
		sql.append(getQuery().addDynFilterSql(joinFieldMetaData, joinFilters));
		if (null != filter) {
			sql.append(filter.getJoinSql());
		}
		return sql.toString();
	}
	
	protected StringBuilder createJoinSql() {
		return getQuery().createMTMJoinSql(joinFieldMetaData, !isInner());
	}

	@Override
	public DMLFilter on(String fieldName, FilterType filter, Object value) {
		tasks.add(() -> {
			Class<?> clz = getEntityClz();
			String field = getQuery().getFieldByReg(fieldName);
			DMLObject.checkField(clz, field);
			if (!joinFilters.containsKey(clz)) {
				joinFilters.put(clz, new HashMap<String, List<DynamicFilterDescriptor>>());
			}
			if (!joinFilters.get(clz).containsKey(field)) {
				joinFilters.get(clz).put(field, new ArrayList<DynamicFilterDescriptor>());
			}
			joinFilters.get(clz).get(field).add(new DynamicFilterDescriptor(fieldName, filter, value, !field.equals(fieldName)));
		});
		return this;
	}

}

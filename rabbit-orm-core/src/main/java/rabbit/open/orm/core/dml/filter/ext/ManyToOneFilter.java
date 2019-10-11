package rabbit.open.orm.core.dml.filter.ext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.InvalidFetchOperationException;
import rabbit.open.orm.core.dml.CallBackTask;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.DynamicFilterTask;
import rabbit.open.orm.core.dml.filter.DMLFilter;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;

/**
 * <b>@description 多对一过滤条件 </b>
 */
public class ManyToOneFilter extends DMLFilter {
	
	// 动态添加的过滤条件
	protected Map<String, List<DynamicFilterDescriptor>> addedFilters;
	
	// entityClz在parentClz中的属性名
	private String fieldName;

	public ManyToOneFilter(Class<?> entityClz, String fieldName, boolean inner) {
		super(entityClz, inner);
		this.fieldName = fieldName;
	}
	
	public ManyToOneFilter(Class<?> entityClz, String fieldName) {
		this(entityClz, fieldName, true);
	}
	
	public ManyToOneFilter(Class<?> entityClz) {
		this(entityClz, null, true);
	}
	
	@Override
	public String getJoinSql() {
		runCallTasks();
		StringBuilder sql = new StringBuilder(isInner() ? " INNER JOIN " : " LEFT JOIN ");
		String tableName = MetaData.getTableNameByClass(getEntityClz());
		sql.append(tableName + " ");
		String alias = getQuery().getAliasByTableName(tableName);
		String parentAlias = getQuery().getAliasByTableName(getQuery().getMetaData().getTableName());
		FieldMetaData fmd = MetaData.getCachedFieldMetaByType(getParentEntityClz(), getEntityClz());
		sql.append(alias + " ON " + parentAlias + "." + getQuery().getColumnName(fmd.getColumn()));
		Column column = MetaData.getPrimaryKeyFieldMeta(getEntityClz()).getColumn();
        String fkName = getQuery().getColumnName(column);
		sql.append(" = " + alias + "." + fkName + " ");
		sql.append(getQuery().createDynamicFilterByClz(getEntityClz(), addedFilters));
		filters.forEach(f -> sql.append(f.getJoinSql()));
		return sql.toString();
	}
	
	@Override
	public void runCallTasks() {
		addedFilters = new HashMap<>();
		checkFieldName();
		DMLObject.checkField(parentEntityClz, fieldName);
		for (CallBackTask task : tasks) {
			task.run();
		}
		for (DMLFilter filter : combinedFilters) {
			ManyToOneFilter f = (ManyToOneFilter) filter;
			f.runCallTasks();
			f.addedFilters.forEach((k, v) -> {
				if (addedFilters.containsKey(k)) {
					addedFilters.get(k).addAll(v);
				} else {
					addedFilters.put(k, v);
				}
			});
		}
	}

	// 核对过滤条件的正确性，并检查fieldName是否有值，如果没有则找出关联值
	private void checkFieldName() {
		if (!isFetchEnabled(getEntityClz())) {
			throw new InvalidFetchOperationException("invalid fetch from " + getParentEntityClz() 
				+ " to " + getEntityClz());
		}
		if (null == fieldName) {
			FieldMetaData meta = MetaData.getCachedFieldMetaByType(getParentEntityClz(), getEntityClz());
			if (meta.isMultiFetchField()) {
				// 这种字段在父类中存在多个，不允许采用匿名方式添加过滤条件
				throw new InvalidFetchOperationException("multi type[" + getEntityClz() 
					+ "] is found in " + getParentEntityClz());
			}
			fieldName = meta.getField().getName();
		}
	}
	
	private boolean isFetchEnabled(Class<?> key) {
		return getQuery().getClzesEnabled2Join().containsKey(key);
	}

	@Override
	public DMLFilter on(String fieldName, FilterType filter, Object value) {
		tasks.add(() -> {
			DynamicFilterTask.doValueCheck(value, filter);
			Class<?> clz = getEntityClz();
			String field = getQuery().getFieldByReg(fieldName);
			DMLObject.checkField(clz, field);
			if (!addedFilters.containsKey(field)) {
				addedFilters.put(field, new ArrayList<>());
			}
			addedFilters.get(field).add(new DynamicFilterDescriptor(fieldName, filter, value, !field.equals(fieldName)));
		});
		return this;
	}

}

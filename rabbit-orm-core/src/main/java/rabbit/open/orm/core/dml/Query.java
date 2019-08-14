package rabbit.open.orm.core.dml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.EmptyListFilterException;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.core.dml.meta.JoinFilterBuilder;

/**
 * <b>Description: 	查询操作</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public class Query<T> extends AbstractQuery<T> {

	public Query(SessionFactory fatory, Class<T> clz) {
		super(fatory, clz);
	}

	public Query(SessionFactory fatory, T filterData, Class<T> clz) {
		super(fatory, filterData, clz);
	}

	/**
	 * 
	 * <b>Description:	动态新增内连接过滤条件</b><br>
	 * @param reg
	 * @param value
	 * @param ft
	 * @param depsPath	依赖路径
	 * @return	
	 * 
	 */
	@Override
	public AbstractQuery<T> addFilter(String reg, Object value, FilterType ft,
			Class<?>... depsPath) {
		if (FilterType.IN.equals(ft)) {
			if (null == value) {
				throw new EmptyListFilterException("filter can't be empty");
			}
			if (value.getClass().isArray()) {
				Object[] arr = (Object[]) value;
				if (arr.length == 0) {
					throw new EmptyListFilterException("filter list size can't be empty");
				}
			}
			if (value instanceof Collection) {
				@SuppressWarnings("unchecked")
				Collection<Object> arr = (Collection<Object>) value;
				if (arr.isEmpty()) {
					throw new EmptyListFilterException("filter list size can't be empty");
				}
			}
		}
		filterTasks.add(new DynamicFilterTask<T>(this, reg, value, ft, depsPath));
		return this;
	}
	
	@Override
	public AbstractQuery<T> addFilter(String reg, Object value,
			Class<?>... depsPath) {
		addFilter(reg, value, FilterType.EQUAL, depsPath);
		return this;
	}

	/**
	 * 
	 * <b>Description:	新增多对多/一对多过滤条件</b><br>
	 * @param reg
	 * @param ft
	 * @param value
	 * @param target
	 * @return	
	 * 
	 */
	@Override
	public AbstractQuery<T> addJoinFilter(String reg, FilterType ft,
			Object value, Class<?> target) {
		String field = getFieldByReg(reg);
		checkJoinFilterClass(target);
		checkField(target, field);
		if(!addedJoinFilters.containsKey(target)){
			addedJoinFilters.put(target, new HashMap<String, List<DynamicFilterDescriptor>>());
		}
		if(!addedJoinFilters.get(target).containsKey(field)){
			addedJoinFilters.get(target).put(field, new ArrayList<DynamicFilterDescriptor>());
		}
		addedJoinFilters.get(target).get(field).add(new DynamicFilterDescriptor(reg, ft, 
				value, !field.equals(reg)));
		return this;
	}

	@Override
	public AbstractQuery<T> addJoinFilter(String reg, Object value,
			Class<?> target) {
		addJoinFilter(reg, FilterType.EQUAL, value, target);
		return this;
	}

	/**
	 * 
	 * <b>Description:	添加内链接过滤条件，相同target的内链接过滤条件和合并</b><br>
	 * @param reg
	 * @param ft
	 * @param value
	 * @param target   多端实体的class对象
	 * @return	
	 * 
	 */
	@Override
	public AbstractQuery<T> addInnerJoinFilter(String reg, FilterType ft,
			Object value, Class<?> target) {
	    if(!joinFilters.containsKey(target)) {
	        addInnerJoinFilter(JoinFilterBuilder.prepare(this).join(target).on(reg, value, ft).build());
	    } else {
	        joinFilters.get(target).getFilterDescriptor().on(reg, value, ft);
	    }
		return this;
	}

	@Override
	public AbstractQuery<T> addInnerJoinFilter(String reg, Object value,
			Class<?> target) {
		addInnerJoinFilter(reg, FilterType.EQUAL, value, target);
		return this;
	}

	/**
	 * 
	 * <b>Description:	新增空查询条件</b><br>
	 * @param reg
	 * @param isNull
	 * @param depsPath
	 * @return	
	 * 
	 */
	@Override
	public AbstractQuery<T> addNullFilter(String reg, boolean isNull, Class<?>... depsPath) {
		addFilter(reg, null, isNull ? FilterType.IS : FilterType.IS_NOT, depsPath);
		return this;
	}

	/**
	 * 
	 * <b>Description:	新增空查询条件</b><br>
	 * @param reg
	 * @param depsPath
	 * @return	
	 * 
	 */
	@Override
	public AbstractQuery<T> addNullFilter(String reg, Class<?>... depsPath) {
		return addNullFilter(reg, true, depsPath);
	}

	@Override
	public AbstractQuery<T> groupBy(String... fields) {
		return this;
	}

}

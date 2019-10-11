package rabbit.open.orm.core.dml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;

/**
 * <b>Description: 	查询操作</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public class Query<T> extends AbstractQuery<T> {

	public Query(SessionFactory factory, Class<T> clz) {
		super(factory, clz);
	}

	public Query(SessionFactory factory, T filterData, Class<T> clz) {
		super(factory, filterData, clz);
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
		filterTasks.add(new DynamicFilterTask<T>(this, reg, value, ft, depsPath));
		return this;
	}

	/**
	 * 添加过滤条件
	 * @param reg
	 * @param value
	 * @param depsPath
	 * @return
	 */
	@Override
	public AbstractQuery<T> addFilter(String reg, Object value,
			Class<?>... depsPath) {
		return addFilter(reg, value, FilterType.EQUAL, depsPath);
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
	public AbstractQuery<T> addJoinFilter(String reg, FilterType ft, Object value, Class<?> target) {
		String field = getFieldByReg(reg);
		checkJoinFilterClass(target);
		checkField(target, field);
		if (!addedJoinFilters.containsKey(target)) {
			addedJoinFilters.put(target, new HashMap<String, List<DynamicFilterDescriptor>>());
		}
		if (!addedJoinFilters.get(target).containsKey(field)) {
			addedJoinFilters.get(target).put(field, new ArrayList<DynamicFilterDescriptor>());
		}
		addedJoinFilters.get(target).get(field).add(new DynamicFilterDescriptor(reg, ft, value, !field.equals(reg)));
		return this;
	}

	/**
	 *
	 * <b>Description:	添加【一对多/多对多】多端左链接过滤条件，相同target的左链接过滤条件和合并</b><br>
	 * @param reg		字段在正则
	 * @param value		条件值
	 * @param target   多端实体的class对象
	 * @return
	 *
	 */
	@Override
	public AbstractQuery<T> addJoinFilter(String reg, Object value,
			Class<?> target) {
		return addJoinFilter(reg, FilterType.EQUAL, value, target);
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
		return addFilter(reg, null, isNull ? FilterType.IS : FilterType.IS_NOT, depsPath);
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

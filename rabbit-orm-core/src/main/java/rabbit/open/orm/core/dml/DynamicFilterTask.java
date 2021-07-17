package rabbit.open.orm.core.dml;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.EmptyListFilterException;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;

import java.util.*;

/**
 * <b>Description: 	动态添加inner过滤条件的任务</b><br>
 *                  
 *                  添加动态过滤条件会触发别名机制，如过先调用addFilter再调用alias就可能出错
 *                  所以新增回调任务来解决这个问题
 * 
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public class DynamicFilterTask<T> implements CallBackTask {

	private final DMLObject<T> dmlAdapter;

	private String reg;

	private Object value;

	private FilterType ft;

	private Class<?>[] depsPath;

	public DynamicFilterTask(DMLObject<T> adapter, String reg, Object value, FilterType ft, Class<?>[] depsPath) {
		this.dmlAdapter = adapter;
		this.reg = reg;
		this.value = value;
		this.ft = ft;
		this.depsPath = depsPath;
	}

	/**
	 * <b>Description: 执行任务</b><br>
	 */
	@Override
	public void run() {
		doValueCheck(value, ft);
		addFilter(reg, value, ft, depsPath);
	}

	public String getReg() {
		return reg;
	}

	public FilterType getFilterType() {
		return ft;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	// 输入校验
	public static void doValueCheck(Object value, FilterType ft) {
		if (!FilterType.IN.equals(ft)) {
			return;
		}
		if (null == value) {
			throw new EmptyListFilterException();
		}
		if (value.getClass().isArray()) {
			Object[] arr = (Object[]) value;
			if (arr.length == 0) {
				throw new EmptyListFilterException();
			}
		}
		if (value instanceof Collection) {
			@SuppressWarnings("unchecked")
			Collection<Object> arr = (Collection<Object>) value;
			if (arr.isEmpty()) {
				throw new EmptyListFilterException();
			}
		}
	}

	private void addFilter(String reg, Object value, FilterType ft, Class<?>... depsPath) {
		if (0 == depsPath.length) {
			addFilter(reg, value, ft, dmlAdapter.getMetaData().getEntityClz());
			return;
		}
		String field = dmlAdapter.getFieldByReg(reg);
		DMLObject.checkField(depsPath[0], field);
		this.dmlAdapter.checkQueryPath(depsPath);
		if (!dmlAdapter.addedFilters.containsKey(depsPath[0])) {
			dmlAdapter.addedFilters.put(depsPath[0], new HashMap<String, List<DynamicFilterDescriptor>>());
		}
		Map<String, List<DynamicFilterDescriptor>> fmps = dmlAdapter.addedFilters.get(depsPath[0]);
		if (!fmps.containsKey(field)) {
			fmps.put(field, new ArrayList<DynamicFilterDescriptor>());
		}
		fmps.get(field).add(new DynamicFilterDescriptor(reg, ft, value, !field.equals(reg)));
	}
}
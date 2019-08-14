package rabbit.open.orm.core.dml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;

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
public class DynamicFilterTask<T> implements CallBackTask{
	
	private final DMLAdapter<T> dmlAdapter;

	private String reg;
	
	private Object value;
	
	private FilterType ft;
	
	private Class<?>[] depsPath;
	
	public DynamicFilterTask(DMLAdapter<T> adapter, String reg, Object value, FilterType ft,
			Class<?>[] depsPath) {
		this.dmlAdapter = adapter;
		this.reg = reg;
		this.value = value;
		this.ft = ft;
		this.depsPath = depsPath;
	}

	/**
	 * 
	 * <b>Description:	执行任务</b><br>	
	 * 
	 */
	@Override
	public void run(){
		addFilter(reg, value, ft, depsPath);
	}
	
	private void addFilter(String reg, Object value, FilterType ft, Class<?>... depsPath) {
		if (depsPath.length == 0) {
			addFilter(reg, value, ft, dmlAdapter.getMetaData().getEntityClz());
			return;
		}
		String field = dmlAdapter.getFieldByReg(reg);
		DMLAdapter.checkField(depsPath[0], field);
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
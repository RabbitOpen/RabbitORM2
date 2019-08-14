package rabbit.open.orm.core.dml.meta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.core.dml.CallBackTask;
import rabbit.open.orm.core.dml.DMLAdapter;

/**
 * <b>Description 多分支的过滤条件，即多个OR条件</b>
 */
public class MultiDropFilter {

    private List<MultiDropFilter> filters;
    
    // 字段名
    private String key;
    
    // 过滤条件描述符
    private FilterDescriptor filterDescriptor;

    protected Class<?> targetClz;
    
    private List<CallBackTask> tasks = new ArrayList<>();
    
    public MultiDropFilter() {
    	filters = new ArrayList<>();
    }

    public MultiDropFilter(String key, FilterDescriptor filterDescriptor) {
		this.key = key;
		this.filterDescriptor = filterDescriptor;
	}

	public MultiDropFilter on(String field, Object value) {
        return on(field, value, FilterType.EQUAL);
    }

    public MultiDropFilter on(String field, Object value, FilterType filterType) {
    	tasks.add(new CallBackTask() {
			@Override
			public void run() {
				Field f = DMLAdapter.checkField(targetClz, field);
				FilterDescriptor fd = new FilterDescriptor(field, value, filterType.value());
				fd.setField(f);
				filters.add(new MultiDropFilter(field, fd));
			}
		});
        return this;
    }

    public List<MultiDropFilter> getFilters() {
        return filters;
    }
    
    public Class<?> getTargetClz() {
        return targetClz;
    }
    
    public void setTargetClz(Class<?> targetClz) {
		this.targetClz = targetClz;
		filters.clear();
		for (CallBackTask task : tasks) {
			task.run();
		}
	}
    
    public String getKey() {
		return key;
	}
    
    public FilterDescriptor getFilterDescriptor() {
		return filterDescriptor;
	}
    
}

package rabbit.open.orm.dml.meta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rabbit.open.orm.annotation.FilterType;
import rabbit.open.orm.dml.CallBackTask;
import rabbit.open.orm.dml.DMLAdapter;

/**
 * <b>Description 多分支的过滤条件，即多个OR条件</b>
 */
public class MultiDropFilter {

    private Map<String, FilterDescriptor> filters;

    protected Class<?> targetClz;
    
    private List<CallBackTask> tasks = new ArrayList<>();
    
    public MultiDropFilter() {
    	filters = new HashMap<>();
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
				filters.put(field, fd);
			}
		});
        return this;
    }

    public Map<String, FilterDescriptor> getFilters() {
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
    
}

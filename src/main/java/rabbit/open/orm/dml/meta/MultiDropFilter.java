package rabbit.open.orm.dml.meta;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import rabbit.open.orm.annotation.FilterType;
import rabbit.open.orm.dml.DMLAdapter;

/**
 * <b>Description 多分支的过滤条件，即多个OR条件</b>
 */
public class MultiDropFilter {

    private Map<String, FilterDescriptor> filters;

    protected Class<?> targetClz;
    
    public MultiDropFilter(Class<?> clz) {
        filters = new HashMap<>();
        this.targetClz = clz;
    }

    public MultiDropFilter on(String field, Object value) {
        return on(field, value, FilterType.EQUAL);
    }

    public MultiDropFilter on(String field, Object value, FilterType filterType) {
        Field f = DMLAdapter.checkField(targetClz, field);
        FilterDescriptor fd = new FilterDescriptor(field, value, filterType.value());
        fd.setField(f);
        this.filters.put(field, fd);
        return this;
    }

    public Map<String, FilterDescriptor> getFilters() {
        return filters;
    }
    
    public Class<?> getTargetClz() {
        return targetClz;
    }
}

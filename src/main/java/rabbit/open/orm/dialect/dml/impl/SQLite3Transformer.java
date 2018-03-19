package rabbit.open.orm.dialect.dml.impl;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import rabbit.open.orm.dml.AbstractQuery;
import rabbit.open.orm.dml.DMLAdapter;
import rabbit.open.orm.dml.DialectTransformer;
import rabbit.open.orm.dml.filter.PreparedValue;
import rabbit.open.orm.exception.RabbitDMLException;

/**
 * <b>Description  SQLite3方言转换器</b>
 */
public class SQLite3Transformer extends DialectTransformer {

    @Override
    public StringBuilder createPageSql(AbstractQuery<?> query) {
        int pageSize = getPageSize(query);
        int pageIndex = getPageIndex(query);
        List<Object> preparedValues = getPreparedValues(query);
        preparedValues.add(new PreparedValue(pageSize));
        preparedValues.add(new PreparedValue(pageIndex * pageSize));
        return getSql(query).append(" LIMIT ? offset ?");
    }

    @Override
    public void setValue2Field(Object target, Field field, Object value) {
        if (Date.class.equals(field.getType())) {
            try {
                field.set(target, new SimpleDateFormat(DMLAdapter.DEFAULT_DATE_PATTERN).parse((String) value));
            } catch (Exception e) {
                throw new RabbitDMLException(e.getMessage(), e);
            }
        } else {
            super.setValue2Field(target, field, value);
        }
        
    }
}

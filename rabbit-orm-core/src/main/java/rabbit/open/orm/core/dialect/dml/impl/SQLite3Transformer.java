package rabbit.open.orm.core.dialect.dml.impl;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dialect.page.Pager;
import rabbit.open.orm.core.dialect.page.impl.SQLite3Pager;
import rabbit.open.orm.core.dml.AbstractQuery;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.DialectTransformer;
import rabbit.open.orm.core.dml.PreparedValue;

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
        return getPager().doPage(getSql(query));
    }

    @Override
    public void setValue2EntityField(Object target, Field field, Object value) {
        field.setAccessible(true);
        if (Date.class.equals(field.getType())) {
            try {
                field.set(target, new SimpleDateFormat(DMLObject.DEFAULT_DATE_PATTERN).parse((String) value));
            } catch (Exception e) {
                throw new RabbitDMLException(e.getMessage(), e);
            }
        } else {
            super.setValue2EntityField(target, field, value);
        }
    }

    @Override
	public Pager createPager() {
		return new SQLite3Pager();
	}
}

package rabbit.open.orm.dialect.dml.impl;

import java.util.List;

import rabbit.open.orm.dml.AbstractQuery;
import rabbit.open.orm.dml.DialectTransformer;
import rabbit.open.orm.dml.filter.PreparedValue;

public class DB2Transformer extends DialectTransformer {

    /**
     * 
     * <b>Description:  根据数据库的不同，将字段sql片段进行转换</b><br>.
     * @param query
     * @return  
     * 
     */
    @Override
    public StringBuilder completeFieldsSql(AbstractQuery<?> query) {
        if (doPage(query)) {
            getSql(query).append(", ROW_NUMBER() OVER(");
            getSql(query).append(generateOrderSql(query));
            getSql(query).append(") AS RN ");
        }
        return super.completeFieldsSql(query);
    }

    /**
     * 
     * <b>Description:  生成排序的sql</b><br>
     * @param query
     * @return  
     * 
     */
    @Override
    public StringBuilder createOrderSql(AbstractQuery<?> query) {
        if (doPage(query)) {
            return new StringBuilder();
        }
        return super.createOrderSql(query);
    }

    private StringBuilder generateOrderSql(AbstractQuery<?> query) {
        return super.createOrderSql(query);
    }

    /**
     * 
     * <b>Description:  创建分页sql</b><br>.
     * @param query
     * @return  
     * 
     */
    @Override
	public StringBuilder createPageSql(AbstractQuery<?> query) {
        int pageSize = getPageSize(query);
        int pageIndex = getPageIndex(query);
        List<Object> preparedValues = getPreparedValues(query);
        long start = 1L + pageIndex * pageSize;
        long end = (1L + pageIndex) * pageSize;
        preparedValues.add(new PreparedValue(start));
        preparedValues.add(new PreparedValue(end));
        return new StringBuilder("SELECT * FROM (" + getSql(query) + ")T WHERE RN BETWEEN ? AND ?");
    }

}

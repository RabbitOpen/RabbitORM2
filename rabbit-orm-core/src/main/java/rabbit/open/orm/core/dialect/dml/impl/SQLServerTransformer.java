package rabbit.open.orm.core.dialect.dml.impl;

import java.util.List;

import rabbit.open.orm.core.dml.AbstractQuery;
import rabbit.open.orm.core.dml.DialectTransformer;
import rabbit.open.orm.core.dml.filter.PreparedValue;

public class SQLServerTransformer extends DialectTransformer {

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
            int offset = getOffset(query);
            getSql(query).insert( offset, "TOP " + ((getPageIndex(query) + 1) * getPageSize(query)) + " ");
            getSql(query).append(", ROW_NUMBER() OVER(");
            getSql(query).append(generateOrderSql(query));
            getSql(query).append(") AS RN ");
        }
        return super.completeFieldsSql(query);
    }

    private int getOffset(AbstractQuery<?> query) {
        if (distinct(query)) {
            return getSql(query).toString().toUpperCase().indexOf("DISTINCT")
                    + "DISTINCT".length() + 1;
        } else {
            return 0;
        }
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
        if (!doOrder(query)) {
            return new StringBuilder("ORDER BY " + getAliasByTableName(query, query.getMetaData().getTableName()) 
                    + "." + query.getSessionFactory().getColumnName(query.getMetaData().getPrimaryKey()));
        }
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
        return new StringBuilder("WITH T AS (" + getSql(query) + ") SELECT * FROM T WHERE RN BETWEEN ? AND ?");
    }
    
}

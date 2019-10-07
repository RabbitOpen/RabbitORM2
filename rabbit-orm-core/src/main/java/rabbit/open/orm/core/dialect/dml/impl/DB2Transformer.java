package rabbit.open.orm.core.dialect.dml.impl;

import rabbit.open.orm.core.dialect.page.Pager;
import rabbit.open.orm.core.dialect.page.impl.DB2Pager;
import rabbit.open.orm.core.dml.AbstractQuery;
import rabbit.open.orm.core.dml.DialectTransformer;

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
        setStartAndEndPreparedValues(query);
        return getPager().doPage(getSql(query));
    }

	@Override
	public Pager createPager() {
		return new DB2Pager();
	}
    
}

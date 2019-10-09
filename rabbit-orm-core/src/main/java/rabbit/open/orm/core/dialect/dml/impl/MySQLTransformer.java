package rabbit.open.orm.core.dialect.dml.impl;

import java.util.List;

import rabbit.open.orm.core.dialect.page.Pager;
import rabbit.open.orm.core.dialect.page.impl.MySQLPager;
import rabbit.open.orm.core.dml.AbstractQuery;
import rabbit.open.orm.core.dml.DialectTransformer;
import rabbit.open.orm.core.dml.PreparedValue;

/**
 * <b>Description: 	mysql方言转换器</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public class MySQLTransformer extends DialectTransformer {

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
	    preparedValues.add(new PreparedValue(pageIndex * pageSize));
        preparedValues.add(new PreparedValue(pageSize));
        return getPager().doPage(getSql(query));
    }

	@Override
	public Pager createPager() {
		return new MySQLPager();
	}
	
}

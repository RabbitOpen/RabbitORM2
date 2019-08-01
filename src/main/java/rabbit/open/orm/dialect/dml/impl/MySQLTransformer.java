package rabbit.open.orm.dialect.dml.impl;

import java.util.List;

import rabbit.open.orm.dml.AbstractQuery;
import rabbit.open.orm.dml.DialectTransformer;
import rabbit.open.orm.dml.filter.PreparedValue;

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
        return getSql(query).append(" LIMIT ?, ?");
    }
	
}

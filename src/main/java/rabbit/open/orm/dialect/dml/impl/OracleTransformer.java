package rabbit.open.orm.dialect.dml.impl;

import java.util.List;

import rabbit.open.orm.dml.AbstractQuery;
import rabbit.open.orm.dml.DialectTransformer;
import rabbit.open.orm.dml.filter.PreparedValue;

public class OracleTransformer extends DialectTransformer{

    /**
     * 
     * <b>Description:  根据数据库的不同，将字段sql片段进行转换</b><br>.
     * @param query
     * @return  
     * 
     */
    @Override
    public StringBuilder completeFieldsSql(AbstractQuery<?> query) {
        if (doPage(query) && !doOrder(query)) {
            getSql(query).append(", ROWNUM AS RN");
        }
        return super.completeFieldsSql(query);
    }

    /**
     * 
     * <b>Description:  创建分页sql</b><br>.
     * @param query
     * @return  
     * 
     */
    @Override
    public StringBuilder createPageSql(AbstractQuery<?> query){
        int pageSize = getPageSize(query);
        int pageIndex = getPageIndex(query);
        List<Object> preparedValues = getPreparedValues(query);
        StringBuilder sql = getSql(query);
        long start = 1L + pageIndex * pageSize;
        long end = (1L + pageIndex) * pageSize;
        preparedValues.add(new PreparedValue(start));
        preparedValues.add(new PreparedValue(end));
        if(!doOrder(query)){
            sql = new StringBuilder("SELECT * FROM (")
                .append(sql)
                .append(") T WHERE RN BETWEEN ? AND ?");
        }else{
            sql = new StringBuilder("SELECT T.*, ROWNUM AS RN FROM (")
                .append(sql + ")T ");
            sql = new StringBuilder("SELECT * FROM (")
                .append(sql)
                .append(") T WHERE RN BETWEEN ? AND ?");
        }
        return sql;
    }

}

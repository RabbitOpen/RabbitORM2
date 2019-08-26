package rabbit.open.orm.core.dialect.dml.impl;

import rabbit.open.orm.core.dml.AbstractQuery;
import rabbit.open.orm.core.dml.DialectTransformer;

public class OracleTransformer extends DialectTransformer {

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
    public StringBuilder createPageSql(AbstractQuery<?> query) {
        setStartAndEndPreparedValues(query);
        StringBuilder sql = getSql(query);
        if (!doOrder(query)) {
            sql = new StringBuilder("SELECT * FROM (")
                .append(sql)
                .append(") T WHERE RN BETWEEN ? AND ?");
        } else {
            sql = new StringBuilder("SELECT T.*, ROWNUM AS RN FROM (")
                .append(sql + ")T ");
            sql = new StringBuilder("SELECT * FROM (")
                .append(sql)
                .append(") T WHERE RN BETWEEN ? AND ?");
        }
        return sql;
    }

}

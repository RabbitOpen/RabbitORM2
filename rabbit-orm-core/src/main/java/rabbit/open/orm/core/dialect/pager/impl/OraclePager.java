package rabbit.open.orm.core.dialect.pager.impl;

import rabbit.open.orm.core.dialect.pager.Pager;

public class OraclePager implements Pager {

	@Override
	public StringBuilder doPage(StringBuilder sql) {
		if (!isOrdered(sql)) {
            return new StringBuilder("SELECT * FROM (")
                .append(sql)
                .append(") T WHERE RN BETWEEN ? AND ?");
        } else {
            StringBuilder temp = new StringBuilder("SELECT T.*, ROWNUM AS RN FROM (")
                .append(sql + ")T ");
            return new StringBuilder("SELECT * FROM (")
                .append(temp)
                .append(") T WHERE RN BETWEEN ? AND ?");
        }
	}
	
	// 判断sql是否排序
	private boolean isOrdered(StringBuilder sql) {
		return sql.toString().toLowerCase().trim().replaceAll("\\s+", " ").contains(" order by ");
	}
}

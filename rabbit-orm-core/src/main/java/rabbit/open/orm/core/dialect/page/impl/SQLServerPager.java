package rabbit.open.orm.core.dialect.page.impl;

import rabbit.open.orm.core.dialect.page.Pager;

public class SQLServerPager implements Pager {

	@Override
	public StringBuilder doPage(StringBuilder sql) {
		return new StringBuilder("WITH T AS (" + sql + ") SELECT * FROM T WHERE RN BETWEEN ? AND ?");
	}

	
}

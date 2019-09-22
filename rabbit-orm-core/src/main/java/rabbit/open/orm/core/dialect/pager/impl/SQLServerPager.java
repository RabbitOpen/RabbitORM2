package rabbit.open.orm.core.dialect.pager.impl;

import rabbit.open.orm.core.dialect.pager.Pager;

public class SQLServerPager implements Pager {

	@Override
	public StringBuilder doPage(StringBuilder sql) {
		return new StringBuilder("WITH T AS (" + sql + ") SELECT * FROM T WHERE RN BETWEEN ? AND ?");
	}

	
}

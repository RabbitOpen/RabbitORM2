package rabbit.open.orm.core.dialect.page.impl;

import rabbit.open.orm.core.dialect.page.Pager;

public class DB2Pager implements Pager {

	@Override
	public StringBuilder doPage(StringBuilder sql) {
		return new StringBuilder("SELECT * FROM (" + sql + ")T WHERE RN BETWEEN ? AND ?");
	}

}

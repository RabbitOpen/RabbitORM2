package rabbit.open.orm.core.dialect.pager.impl;

import rabbit.open.orm.core.dialect.pager.Pager;

public class DB2Pager implements Pager {

	@Override
	public StringBuilder doPage(StringBuilder sql) {
		return new StringBuilder("SELECT * FROM (" + sql + ")T WHERE RN BETWEEN ? AND ?");
	}

}

package rabbit.open.orm.core.dialect.pager.impl;

import rabbit.open.orm.core.dialect.pager.Pager;

public class MySQLPager implements Pager {

	@Override
	public StringBuilder doPage(StringBuilder sql) {
		return sql.append(" LIMIT ?, ?");
	}

}

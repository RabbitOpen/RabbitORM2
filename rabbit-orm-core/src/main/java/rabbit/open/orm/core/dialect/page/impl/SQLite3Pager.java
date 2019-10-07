package rabbit.open.orm.core.dialect.page.impl;

import rabbit.open.orm.core.dialect.page.Pager;

public class SQLite3Pager implements Pager {

	@Override
	public StringBuilder doPage(StringBuilder sql) {
		return sql.append(" LIMIT ? offset ?");
	}

}

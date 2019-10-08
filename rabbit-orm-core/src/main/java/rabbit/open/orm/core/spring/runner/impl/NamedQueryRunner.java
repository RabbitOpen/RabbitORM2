package rabbit.open.orm.core.spring.runner.impl;

import rabbit.open.orm.core.dialect.page.PageHelper;
import rabbit.open.orm.core.dialect.page.PageInfo;
import rabbit.open.orm.core.dml.NamedQuery;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.runner.MethodMapping;
import rabbit.open.orm.core.spring.runner.SQLRunner;

/**
 * <b>@description NamedQueryRunner </b>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class NamedQueryRunner extends SQLRunner {

	@Override
	public Object run(Object[] args, MethodMapping mapping, Class<?> namespaceClz, SessionFactory factory) {
		NamedQuery query = new NamedQuery(factory, namespaceClz, mapping.getSqlName());
		for (int i = 0; i < mapping.getParaNames().size(); i++) {
			query.set(mapping.getParaNames().get(i), args[i], null, null);
		}
		if (PageHelper.isPaged()) {
			PageInfo pageInfo = PageHelper.getPageInfo();
			query.page(pageInfo.getPageIndex(), pageInfo.getPageSize());
		}
		return query.unique();
	}

}

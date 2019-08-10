package rabbit.open.orm.core.spring.runner.impl;

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
		NamedQuery namedQuery = new NamedQuery(factory, namespaceClz, mapping.getSqlName());
		for (int i = 0; i < mapping.getParaNames().size(); i++) {
			namedQuery.set(mapping.getParaNames().get(i), args[i]);
		}
		return namedQuery.unique();
	}

}

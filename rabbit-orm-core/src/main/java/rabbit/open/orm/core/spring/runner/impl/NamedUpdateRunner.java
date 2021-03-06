package rabbit.open.orm.core.spring.runner.impl;

import rabbit.open.orm.core.dml.NamedUpdate;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.runner.MethodMapping;
import rabbit.open.orm.core.spring.runner.SQLRunner;

/**
 * <b>@description NamedUpdateRunner </b>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class NamedUpdateRunner extends SQLRunner {

	@Override
	public Object run(Object[] args, MethodMapping mapping, Class<?> namespaceClz, SessionFactory factory) {
		NamedUpdate update = new NamedUpdate(factory, namespaceClz, mapping.getSqlName());
		for (int i = 0; i < mapping.getParaNames().size(); i++) {
			update.set(mapping.getParaNames().get(i), args[i], null, null);
		}
		return update.execute();
	}

}

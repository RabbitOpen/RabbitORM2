package rabbit.open.orm.core.spring.runner.impl;

import rabbit.open.orm.core.dml.NamedDelete;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.runner.MethodMapping;
import rabbit.open.orm.core.spring.runner.SQLRunner;

/**
 * <b>@description NamedDeleteRunner </b>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class NamedDeleteRunner extends SQLRunner {

	@Override
	public Object run(Object[] args, MethodMapping mapping, Class<?> namespaceClz, SessionFactory factory) {
		NamedDelete namedDelete = new NamedDelete(factory, namespaceClz, mapping.getSqlName());
		for (int i = 0; i < mapping.getParaNames().size(); i++) {
			namedDelete.set(mapping.getParaNames().get(i), args[i]);
		}
		return namedDelete.execute();
	}

}

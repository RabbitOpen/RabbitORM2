package rabbit.open.orm.core.spring.runner.impl;

import java.util.List;

import rabbit.open.orm.core.dml.SQLQuery;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.runner.MethodMapping;
import rabbit.open.orm.core.spring.runner.SQLRunner;

/**
 * <b>@description NamedJdbcRunner </b>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class NamedJdbcRunner extends SQLRunner {

	@Override
	public Object run(Object[] args, MethodMapping mapping, Class<?> namespaceClz, SessionFactory factory) {
		SQLQuery namedJdbc = new SQLQuery(factory, namespaceClz, mapping.getSqlName());
		for (int i = 0; i < mapping.getParaNames().size(); i++) {
			namedJdbc.set(mapping.getParaNames().get(i), args[i]);
		}
		if (nonQuery(mapping)) {
			// long int 等
			return namedJdbc.add();
		} else if (List.class.isAssignableFrom(mapping.getReturnType())) {
			return namedJdbc.list();
		} else {
			return namedJdbc.unique();
		}
	}

	private boolean nonQuery(MethodMapping mapping) {
		return mapping.getReturnType().isPrimitive() || Number.class.isAssignableFrom(mapping.getReturnType());
	}

}

package rabbit.open.orm.core.spring.runner.impl;

import java.util.List;

import rabbit.open.orm.common.exception.InvalidReturnTypeException;
import rabbit.open.orm.core.dml.SQLQuery;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.runner.MethodMapping;
import rabbit.open.orm.core.spring.runner.SQLRunner;

/**
 * <b>@description NamedJdbcRunner </b>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SQLQueryRunner extends SQLRunner {

	@Override
	public Object run(Object[] args, MethodMapping mapping, Class<?> namespaceClz, SessionFactory factory) {
		SQLQuery sqlQuery = new SQLQuery(factory, namespaceClz, mapping.getSqlName());
		for (int i = 0; i < mapping.getParaNames().size(); i++) {
			sqlQuery.set(mapping.getParaNames().get(i), args[i]);
		}
		if (invalidReturnType(mapping)) {
			throw new InvalidReturnTypeException(mapping.getSqlName());
		}
		if (List.class.isAssignableFrom(mapping.getReturnType())) {
			return sqlQuery.list();
		} else {
			return sqlQuery.unique();
		}
	}
	
	private boolean invalidReturnType(MethodMapping mapping) {
		return mapping.getReturnType().isPrimitive() || Number.class.isAssignableFrom(mapping.getReturnType());
	}
}

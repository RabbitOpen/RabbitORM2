package rabbit.open.orm.core.spring.runner.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import rabbit.open.orm.common.exception.InvalidReturnTypeException;
import rabbit.open.orm.core.dialect.page.PageHelper;
import rabbit.open.orm.core.dialect.page.PageInfo;
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
			sqlQuery.set(mapping.getParaNames().get(i), args[i], null, null);
		}
		if (invalidReturnType(mapping)) {
			throw new InvalidReturnTypeException(mapping.getSqlName());
		}
		if (PageHelper.isPaged()) {
			PageInfo pageInfo = PageHelper.getPageInfo();
			sqlQuery.page(pageInfo.getPageIndex(), pageInfo.getPageSize());
		}
		if (Collection.class.isAssignableFrom(mapping.getReturnType())) {
			Type type = mapping.getGenericalResultType();
			return sqlQuery.list((Class<?>)((ParameterizedType) type).getActualTypeArguments()[0]);
		} else {
			return sqlQuery.unique(mapping.getReturnType());
		}
	}
	
	private boolean invalidReturnType(MethodMapping mapping) {
		return mapping.getReturnType().isPrimitive() || Number.class.isAssignableFrom(mapping.getReturnType());
	}
}

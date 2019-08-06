package rabbit.open.test.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;

public class BaseService<T> extends SpringDaoAdapter<T> {

	@Resource(name = "sessionFactory")
	protected SessionFactory factory;

	@PostConstruct
	public void setUp() {
		setSessionFactory(factory);
	}

	public SessionFactory getFactory() {
		return factory;
	}
}
package transaction.c3p0test;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;

public class TransactionBaseService<T> extends SpringDaoAdapter<T> {

	@Resource(name = "sessionFactoryC3p0")
	protected SessionFactory factory;

	@PostConstruct
	public void setUp() {
		setSessionFactory(factory);
	}

	public SessionFactory getFactory() {
		return factory;
	}

}

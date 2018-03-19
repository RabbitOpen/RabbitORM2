package sqlite.test.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import rabbit.open.orm.pool.SessionFactory;
import rabbit.open.orm.spring.SpringDaoAdapter;

public class BaseService<T> extends SpringDaoAdapter<T>{

	@Resource(name="sessionFactory-sqlite")
	protected SessionFactory factory;
	
	@PostConstruct
	public void setUp(){
		setSessionFactory(factory);
	}
	
	public SessionFactory getFactory() {
        return factory;
    }
}

package oracle.test.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;

public class OracleBaseService<T> extends SpringDaoAdapter<T>{

    @Resource(name="sessionFactory-oracle")
    protected SessionFactory factory;
	
	@PostConstruct
	public void setUp(){
		setSessionFactory(factory);
	}
	
	public SessionFactory getFactory() {
        return factory;
    }
}

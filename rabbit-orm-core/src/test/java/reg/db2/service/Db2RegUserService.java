package reg.db2.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;
import reg.db2.entity.RegUser;

@Service
public class Db2RegUserService extends SpringDaoAdapter<RegUser> {

	@Resource(name = "sessionFactory4db2")
	protected SessionFactory factory;

	@PostConstruct
	public void setUp() {
		setSessionFactory(factory);
	}

	public SessionFactory getFactory() {
		return factory;
	}
}

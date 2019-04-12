package reg.sqlserver.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import rabbit.open.orm.pool.SessionFactory;
import rabbit.open.orm.spring.SpringDaoAdapter;
import reg.sqlserver.entity.RegUser;


@Service
public class SqlServerRegUserService extends SpringDaoAdapter<RegUser> {
	@Resource(name = "sessionFactorysqlserver")
	protected SessionFactory factory;

	@PostConstruct
	public void setUp() {
		setSessionFactory(factory);
	}

	public SessionFactory getFactory() {
		return factory;
	}
}

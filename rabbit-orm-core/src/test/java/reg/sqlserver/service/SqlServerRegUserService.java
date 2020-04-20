package reg.sqlserver.service;

import org.springframework.stereotype.Service;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;
import reg.sqlserver.entity.RegUser;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;


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

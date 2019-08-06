package reg.sqlserver.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;
import reg.sqlserver.entity.RegRoom;


@Service
public class SqlServerRegRoomService extends SpringDaoAdapter<RegRoom> {

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

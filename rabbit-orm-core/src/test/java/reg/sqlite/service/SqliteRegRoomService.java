package reg.sqlite.service;

import org.springframework.stereotype.Service;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;
import reg.sqlite.entity.RegRoom;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class SqliteRegRoomService extends SpringDaoAdapter<RegRoom> {

	@Resource(name = "sessionFactorysqlite")
	protected SessionFactory factory;

	@PostConstruct
	public void setUp() {
		setSessionFactory(factory);
	}

	public SessionFactory getFactory() {
		return factory;
	}
}

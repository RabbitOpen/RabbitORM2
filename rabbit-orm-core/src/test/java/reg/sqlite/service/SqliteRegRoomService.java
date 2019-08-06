package reg.sqlite.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import rabbit.open.orm.pool.SessionFactory;
import rabbit.open.orm.spring.SpringDaoAdapter;
import reg.sqlite.entity.RegRoom;

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

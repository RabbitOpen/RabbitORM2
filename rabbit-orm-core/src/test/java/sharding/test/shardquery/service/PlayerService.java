package sharding.test.shardquery.service;

import org.springframework.stereotype.Service;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;
import sharding.test.shardquery.entity.Player;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

@Service
public class PlayerService extends SpringDaoAdapter<Player> {

	@Resource(name = "readWriteSplitedSessionFactory1")
	protected SessionFactory factory;
	
	@Resource(name = "ds1")
	private DataSource ds1;
	
	@Resource(name = "ds2")
	private DataSource ds2;

	@PostConstruct
	public void setUp() {
		setSessionFactory(factory);
	}

	public SessionFactory getFactory() {
		return factory;
	}

	public DataSource getDs1() {
		return ds1;
	}

	public DataSource getDs2() {
		return ds2;
	}


	
}

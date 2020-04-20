package sharding.test.db.service;

import org.springframework.stereotype.Service;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;
import sharding.test.db.entity.RWUser;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * <b>Description 只操作写的数据源的服务</b>
 */
@Service
public class WriteOnlyUserService extends SpringDaoAdapter<RWUser> {

	@Resource(name = "writeSessionFactory")
	protected SessionFactory factory;

	@PostConstruct
	public void setUp() {
		setSessionFactory(factory);
	}

	public SessionFactory getFactory() {
		return factory;
	}
}

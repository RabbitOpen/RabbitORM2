package oracle.test.service;

import oracle.test.entity.OracleOrganization;
import org.springframework.stereotype.Service;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class OracleOrganizationService extends SpringDaoAdapter<OracleOrganization> {

	@Resource(name = "sessionFactory-oracle")
	protected SessionFactory factory;

	@PostConstruct
	public void setUp() {
		setSessionFactory(factory);
	}

}

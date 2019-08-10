package test.mapper;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;
import test.mapper.entity.MappingUser;

@Service
public class MapperUserService extends SpringDaoAdapter<MappingUser> {

	@Resource(name = "sessionFactoryMapper")
	private SessionFactory factory;
	
	@PostConstruct
	public void setUp() {
		setSessionFactory(factory);
	}
}

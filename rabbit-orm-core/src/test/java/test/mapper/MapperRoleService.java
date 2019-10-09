package test.mapper;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.spring.SpringDaoAdapter;
import test.mapper.entity.MapperRole;

@Service
public class MapperRoleService extends SpringDaoAdapter<MapperRole> {

	@Resource(name = "sessionFactoryMapper")
	private SessionFactory factory;
	
	@PostConstruct
	public void setUp() {
		setSessionFactory(factory);
	}
}

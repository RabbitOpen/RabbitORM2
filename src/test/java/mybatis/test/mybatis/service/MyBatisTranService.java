package mybatis.test.mybatis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rabbit.open.orm.exception.RabbitDMLException;

@Service
public class MyBatisTranService {

	@Autowired
	MyBatisService service;
	
	@Transactional
	public void tranadd(String name) {
		service.tranAddOrg(name);
		service.tranAddUser(name);
	}

	@Transactional
	public void rollback(String name) {
		service.tranAddOrg(name);
		service.tranAddUser(name);
		throw new RabbitDMLException("");
	}
}

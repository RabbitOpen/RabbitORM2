package mybatis.test.service;

import mybatis.test.entity.MUser;
import mybatis.test.mybatis.service.MyBatisTranService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import rabbit.open.orm.exception.RabbitDMLException;

@Service
public class MUserService extends BaseService<MUser> {

	@Autowired
	MyBatisTranService mbs;
	
	@Autowired
	MOrgService orgService;
	
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void combinedAdd(String username) {
		mbs.tranadd(username);
		add(new MUser(username));
	}

	@Transactional
	public void combinedRollback(String username) {
		mbs.tranadd(username);
		add(new MUser(username));
		throw new RabbitDMLException("");
	}
	
	@Transactional(propagation = Propagation.NESTED)
	public void nestedTranAdd(String name) {
		add(new MUser(name));
		add(new MUser(name));
		throw new RuntimeException();
	}

	@Transactional()
	public void tranAdd(String name) {
		add(new MUser(name));
		add(new MUser(name));
		throw new RuntimeException();
	}
	
}

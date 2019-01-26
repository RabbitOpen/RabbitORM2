package transaction.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rabbit.open.test.service.BaseService;
import transaction.entity.TUser;

@Service
public class TUserService extends BaseService<TUser> {

	@Transactional
	public void simpleTranAdd(String name) {
		createDelete().addFilter("name", name).execute();
		add(new TUser(name));
		add(new TUser(name));
	}
	
	@Transactional
	public void simpleExceptionAdd(String name) {
		createDelete().addFilter("name", name).execute();
		add(new TUser(name));
		add(new TUser(name));
		throw new RuntimeException("u");
	}
}

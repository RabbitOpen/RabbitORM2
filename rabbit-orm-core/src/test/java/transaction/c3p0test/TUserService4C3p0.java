package transaction.c3p0test;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transaction.rabbit.entity.TUser;

@Service
public class TUserService4C3p0 extends TransactionBaseService<TUser> {

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

package transaction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SimpleService {

	@Autowired
	TOrgService os;

	@Autowired
	TUserService us;
	
	public void userRollback(String name) {
		os.simpleTranAdd(name);
		us.simpleExceptionAdd(name);
	}

	@Transactional
	public void simpleTransAdd(String name) {
		os.simpleTranAdd(name);
		us.simpleTranAdd(name);
	}

	@Transactional
	public void simpleTransRollBack(String name) {
		os.simpleTranAdd(name);
		us.simpleExceptionAdd(name);
	}
}

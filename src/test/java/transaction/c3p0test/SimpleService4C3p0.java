package transaction.c3p0test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SimpleService4C3p0 {

	@Autowired
	TOrgService4C3p0 os;

	@Autowired
	TUserService4C3p0 us;
	
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

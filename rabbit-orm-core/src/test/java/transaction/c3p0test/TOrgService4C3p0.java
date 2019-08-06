package transaction.c3p0test;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import transaction.rabbit.entity.XOrg;

@Service
public class TOrgService4C3p0 extends TransactionBaseService<XOrg> {

	@Transactional
	public void simpleTranAdd(String name) {
		createDelete().addFilter("name", name).execute();
		add(new XOrg(name));
		add(new XOrg(name));
	}
	
	@Transactional
	public void simpleExceptionAdd(String name) {
		createDelete().addFilter("name", name).execute();
		add(new XOrg(name));
		add(new XOrg(name));
		throw new RuntimeException("x");
	}
	
}

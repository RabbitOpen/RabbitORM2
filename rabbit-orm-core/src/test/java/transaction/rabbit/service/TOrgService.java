package transaction.rabbit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rabbit.open.test.service.BaseService;
import transaction.rabbit.entity.XOrg;

@Service
public class TOrgService extends BaseService<XOrg> {

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

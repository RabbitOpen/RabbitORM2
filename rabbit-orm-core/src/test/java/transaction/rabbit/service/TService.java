package transaction.rabbit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TService {

	@Autowired
	SimpleService ss;
	
	@Transactional
	public void simpleTransRollBack(String name) {
		ss.simpleTransRollBack(name);
	}
}

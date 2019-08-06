package transaction.c3p0test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TService4C3p0 {

	@Autowired
	SimpleService4C3p0 ss;
	
	@Transactional
	public void simpleTransRollBack(String name) {
		ss.simpleTransRollBack(name);
	}
}

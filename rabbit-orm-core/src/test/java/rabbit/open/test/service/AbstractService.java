package rabbit.open.test.service;

import org.springframework.transaction.annotation.Transactional;
import rabbit.open.test.entity.User;

public abstract class AbstractService extends BaseService<User> {

	@Transactional
	public void batchAdd() {
		for (int i = 0; i < 5; i++) {
			User u = new User();
			u.setAge(i);
			add(u);
			
		}
	}
}
